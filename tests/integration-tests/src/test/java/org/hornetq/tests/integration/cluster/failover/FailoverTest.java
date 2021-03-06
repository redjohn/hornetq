/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.tests.integration.cluster.failover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import junit.framework.Assert;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.Interceptor;
import org.hornetq.api.core.Message;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.MessageHandler;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.client.impl.ClientSessionFactoryInternal;
import org.hornetq.core.server.impl.InVMNodeManager;
import org.hornetq.core.transaction.impl.XidImpl;
import org.hornetq.jms.client.HornetQTextMessage;
import org.hornetq.tests.integration.IntegrationTestLogger;
import org.hornetq.tests.integration.cluster.util.TestableServer;
import org.hornetq.tests.util.CountDownSessionFailureListener;
import org.hornetq.tests.util.RandomUtil;
import org.hornetq.tests.util.TransportConfigurationUtils;

/**
 *
 * A FailoverTest
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class FailoverTest extends FailoverTestBase
{
	private static final IntegrationTestLogger log = IntegrationTestLogger.LOGGER;

	   private static final int NUM_MESSAGES = 100;

	   private ServerLocator locator;
	   private ClientSessionFactoryInternal sf;

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      locator = getServerLocator();
   }

	protected ClientSession createSession(ClientSessionFactory sf,
			boolean autoCommitSends,
			boolean autoCommitAcks,
			int ackBatchSize) throws Exception
	{
		return addClientSession(sf.createSession(autoCommitSends, autoCommitAcks, ackBatchSize));
	}

	protected ClientSession createSession(ClientSessionFactory sf, boolean autoCommitSends, boolean autoCommitAcks) throws Exception
	{
		return addClientSession(sf.createSession(autoCommitSends, autoCommitAcks));
	}

	protected ClientSession createSession(ClientSessionFactory sf) throws Exception
	{
		return addClientSession(sf.createSession());
	}

   protected ClientSession createSession(ClientSessionFactory sf,
                                         boolean xa,
                                         boolean autoCommitSends,
                                         boolean autoCommitAcks) throws Exception
   {
      return addClientSession(sf.createSession(xa, autoCommitSends, autoCommitAcks));
   }

   // https://issues.jboss.org/browse/HORNETQ-685
   public void testTimeoutOnFailover() throws Exception
   {
      locator.setCallTimeout(5000);
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setAckBatchSize(0);
      locator.setReconnectAttempts(-1);
      ((InVMNodeManager)nodeManager).failoverPause = 5000l;

      ClientSessionFactoryInternal sf = (ClientSessionFactoryInternal)locator.createSessionFactory();

      final ClientSession session = createSession(sf, true, true);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      final ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      final CountDownLatch latch = new CountDownLatch(10);

      Runnable r = new Runnable()
      {
         public void run()
         {
            for (int i = 0; i < 500; i++)
            {
               ClientMessage message = session.createMessage(true);
               message.putIntProperty("counter", i);
               try
               {
                  producer.send(message);
                  if (i < 10)
                  {
                     latch.countDown();
                  }
               }
               catch (HornetQException e)
               {
                  // this is our retry
                  try
                  {
                     producer.send(message);
                  }
                  catch (HornetQException e1)
                  {
                     e1.printStackTrace();
                  }
               }
            }
         }
      };
      Thread t = new Thread(r);
      t.start();
      latch.await(10, TimeUnit.SECONDS);
      log.info("crashing session");
      crash(session);
      t.join(5000);
      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);
      session.start();
      for (int i = 0; i < 500; i++)
      {
         ClientMessage m = consumer.receive(1000);
         assertNotNull(m);
         // assertEquals(i, m.getIntProperty("counter").intValue());
      }
   }

   // https://issues.jboss.org/browse/HORNETQ-685
   public void testTimeoutOnFailoverConsume() throws Exception
   {
      locator.setCallTimeout(5000);
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setAckBatchSize(0);
      locator.setBlockOnAcknowledge(true);
      locator.setReconnectAttempts(-1);
      locator.setRetryInterval(500);
      locator.setAckBatchSize(0);
      ((InVMNodeManager)nodeManager).failoverPause = 5000l;

      ClientSessionFactoryInternal sf = (ClientSessionFactoryInternal)locator.createSessionFactory();

      final ClientSession session = createSession(sf, true, true);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      final ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      for (int i = 0; i < 500; i++)
      {
         ClientMessage message = session.createMessage(true);
         message.putIntProperty("counter", i);
         producer.send(message);
      }

      final CountDownLatch latch = new CountDownLatch(1);
      final CountDownLatch endLatch = new CountDownLatch(1);

      final ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);
      session.start();

      final Map<Integer, ClientMessage> received = new HashMap<Integer, ClientMessage>();

      consumer.setMessageHandler(new MessageHandler()
      {

         public void onMessage(ClientMessage message)
         {
            Integer counter = message.getIntProperty("counter");
            received.put(counter, message);
            try
            {
               log.info("acking message = id = " + message.getMessageID() +
                        ", counter = " +
                        message.getIntProperty("counter"));
               message.acknowledge();
            }
            catch (HornetQException e)
            {
               e.printStackTrace();
               return;
            }
            log.info("Acked counter = " + counter);
            if (counter.equals(10))
            {
               latch.countDown();
            }
            if (received.size() == 500)
            {
               endLatch.countDown();
            }
         }

      });
      latch.await(10, TimeUnit.SECONDS);
      log.info("crashing session");
      crash(session);
      endLatch.await(60, TimeUnit.SECONDS);
      assertTrue("received only " + received.size(), received.size() == 500);

      session.close();
   }

   public void testTimeoutOnFailoverConsumeBlocked() throws Exception
   {
      locator.setCallTimeout(5000);
      locator.setBlockOnNonDurableSend(true);
      locator.setConsumerWindowSize(0);
      locator.setBlockOnDurableSend(true);
      locator.setAckBatchSize(0);
      locator.setBlockOnAcknowledge(true);
      locator.setReconnectAttempts(-1);
      locator.setRetryInterval(500);
      locator.setAckBatchSize(0);
      ((InVMNodeManager)nodeManager).failoverPause = 5000l;

      ClientSessionFactoryInternal sf = (ClientSessionFactoryInternal)locator.createSessionFactory();

      final ClientSession session = createSession(sf, true, true);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      final ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      for (int i = 0; i < 500; i++)
      {
         ClientMessage message = session.createMessage(true);
         message.putIntProperty("counter", i);
         message.putBooleanProperty("end", i == 499);
         producer.send(message);
      }

      final CountDownLatch latch = new CountDownLatch(1);
      final CountDownLatch endLatch = new CountDownLatch(1);

      final ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);
      session.start();

      final Map<Integer, ClientMessage> received = new HashMap<Integer, ClientMessage>();

      Thread t = new Thread()
      {
         @Override
         public void run()
         {
            ClientMessage message = null;
            try
            {
               while ((message = getMessage()) != null)
               {
                  Integer counter = message.getIntProperty("counter");
                  received.put(counter, message);
                  try
                  {
                     log.info("acking message = id = " + message.getMessageID() +
                              ", counter = " +
                              message.getIntProperty("counter"));
                     message.acknowledge();
                  }
                  catch (HornetQException e)
                  {
                     e.printStackTrace();
                     continue;
                  }
                  log.info("Acked counter = " + counter);
                  if (counter.equals(10))
                  {
                     latch.countDown();
                  }
                  if (received.size() == 500)
                  {
                     endLatch.countDown();
                  }

                  if (message.getBooleanProperty("end"))
                  {
                     break;
                  }
               }
            }
            catch (Exception e)
            {
               fail("failing due to exception " + e);
            }

         }

         private ClientMessage getMessage()
         {
            while (true)
            {
               try
               {
                  ClientMessage msg = consumer.receive(20000);
                  if (msg == null)
                  {
                     log.info("Returning null message on consuming");
                  }
                  return msg;
               }
               catch (HornetQException ignored)
               {
                  if (ignored.getCode() == HornetQException.OBJECT_CLOSED)
                  {
                     throw new RuntimeException(ignored);
                  }
                  // retry
                  ignored.printStackTrace();
               }
            }
         }
      };
      t.start();
      latch.await(10, TimeUnit.SECONDS);
      log.info("crashing session");
      crash(session);
      endLatch.await(60, TimeUnit.SECONDS);
      t.join();
      assertTrue("received only " + received.size(), received.size() == 500);

      session.close();
   }

   // https://issues.jboss.org/browse/HORNETQ-685
   public void testTimeoutOnFailoverTransactionCommit() throws Exception
   {
      locator.setCallTimeout(2000);
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setAckBatchSize(0);
      locator.setReconnectAttempts(-1);
      ((InVMNodeManager)nodeManager).failoverPause = 5000l;

      ClientSessionFactoryInternal sf = (ClientSessionFactoryInternal)locator.createSessionFactory();

      final ClientSession session = createSession(sf, true, false, false);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      final ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      Xid xid = new XidImpl("uhuhuhu".getBytes(), 126512, "auhsduashd".getBytes());

      session.start(xid, XAResource.TMNOFLAGS);

      for (int i = 0; i < 500; i++)
      {
         ClientMessage message = session.createMessage(true);
         message.putIntProperty("counter", i);

         System.out.println("sending message: " + i);
         producer.send(message);

      }
      session.end(xid, XAResource.TMSUCCESS);
      session.prepare(xid);
      System.out.println("crashing session");
      crash(false, session);

      session.commit(xid, false);

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);
      session.start();
      for (int i = 0; i < 500; i++)
      {
         ClientMessage m = consumer.receive(1000);
         assertNotNull(m);
         assertEquals(i, m.getIntProperty("counter").intValue());
      }
   }

   // https://issues.jboss.org/browse/HORNETQ-685
   public void testTimeoutOnFailoverTransactionRollback() throws Exception
   {
      locator.setCallTimeout(2000);
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setAckBatchSize(0);
      locator.setReconnectAttempts(-1);
      ((InVMNodeManager)nodeManager).failoverPause = 5000l;

      ClientSessionFactoryInternal sf = (ClientSessionFactoryInternal)locator.createSessionFactory();

      final ClientSession session = createSession(sf, true, false, false);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      final ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      Xid xid = new XidImpl("uhuhuhu".getBytes(), 126512, "auhsduashd".getBytes());

      session.start(xid, XAResource.TMNOFLAGS);

      for (int i = 0; i < 500; i++)
      {
         ClientMessage message = session.createMessage(true);
         message.putIntProperty("counter", i);

         System.out.println("sending message: " + i);
         producer.send(message);
      }

      session.end(xid, XAResource.TMSUCCESS);
      session.prepare(xid);
      System.out.println("crashing session");
      crash(false, session);

      session.rollback(xid);

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);
      session.start();

      ClientMessage m = consumer.receive(1000);
      assertNull(m);

   }

   /**
    * @see https://jira.jboss.org/browse/HORNETQ-522
    * @throws Exception
    */
   public void testNonTransactedWithZeroConsumerWindowSize() throws Exception
   {
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setAckBatchSize(0);
      locator.setReconnectAttempts(-1);

      createClientSessionFactory();

      ClientSession session = createSession(sf, true, true);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         ClientMessage message = session.createMessage(true);

         setBody(i, message);

         message.putIntProperty("counter", i);

         producer.send(message);
      }

      int winSize = 0;
      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS, null, winSize, 100, false);

      final List<ClientMessage> received = new ArrayList<ClientMessage>();

      consumer.setMessageHandler(new MessageHandler()
      {

         public void onMessage(ClientMessage message)
         {
            received.add(message);
            try
            {
               Thread.sleep(20);
            }
            catch (InterruptedException e)
            {
               e.printStackTrace();
            }
         }

      });

      session.start();

      crash(session);

      int retry = 0;
      while (received.size() < NUM_MESSAGES)
      {
         Thread.sleep(1000);
         retry++;
         if (retry > 5)
         {
            break;
         }
      }
      System.out.println("received.size() = " + received.size());
      session.close();
      final int retryLimit = 5;
      Assert.assertTrue("Number of retries (" + retry + ") should be <= " + retryLimit, retry <= retryLimit);
   }

   private void createClientSessionFactory() throws Exception
   {
      sf = (ClientSessionFactoryInternal)createSessionFactory(locator);
   }

   public void testNonTransacted() throws Exception
   {

      createSessionFactory();

      ClientSession session = createSession(sf, true, true);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session, producer);

      crash(session);

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      receiveDurableMessages(consumer);

      session.close();

      sf.close();

      Assert.assertEquals(0, sf.numSessions());

      Assert.assertEquals(0, sf.numConnections());
   }

   private void createSessionFactory() throws Exception
   {
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setReconnectAttempts(-1);

      sf = createSessionFactoryAndWaitForTopology(locator, 2);
   }

   public void testConsumeTransacted() throws Exception
   {
      createSessionFactory();

      ClientSession session = createSession(sf, false, false);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      final int numMessages = 10;

      sendMessages(session, producer, numMessages);

      session.commit();

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer.receive(1000);
         assertNotNull("Just crashed? " + (i == 6) + " " + i, message);

         message.acknowledge();

         // TODO: The test won't pass if you uncomment this line
         // assertEquals(i, (int)message.getIntProperty("counter"));

         if (i == 5)
         {
            crash(session);
         }
      }

      try
      {
         session.commit();
         fail("session must have rolled back on failover");
      }
      catch (HornetQException e)
      {
         assertTrue(e.getCode() == HornetQException.TRANSACTION_ROLLED_BACK);
      }

      consumer.close();

      consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer.receive(1000);

         assertNotNull("Expecting message #" + i, message);

         message.acknowledge();
      }

      session.commit();

      session.close();
   }

   // https://jira.jboss.org/jira/browse/HORNETQ-285
   public void testFailoverOnInitialConnection() throws Exception
   {
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setFailoverOnInitialConnection(true);
      locator.setReconnectAttempts(-1);

      sf = createSessionFactoryAndWaitForTopology(locator, 2);

      // Crash live server
      crash();

      ClientSession session = createSession(sf);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);


      sendMessages(session, producer, NUM_MESSAGES);

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      receiveMessages(consumer);

      session.close();
   }

   public void testTransactedMessagesSentSoRollback() throws Exception
   {
      createSessionFactory();

      ClientSession session = createSession(sf, false, false);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session, producer);

      crash(session);

      Assert.assertTrue(session.isRollbackOnly());

      try
      {
         session.commit();

         Assert.fail("Should throw exception");
      }
      catch (HornetQException e)
      {
         Assert.assertEquals(HornetQException.TRANSACTION_ROLLED_BACK, e.getCode());
      }

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      ClientMessage message = consumer.receiveImmediate();

      Assert.assertNull("message should be null! Was: " + message, message);

      session.close();
   }

   /**
    * Test that once the transacted session has throw a TRANSACTION_ROLLED_BACK exception,
    * it can be reused again
    */
   public void testTransactedMessagesSentSoRollbackAndContinueWork() throws Exception
   {
      createSessionFactory();

      ClientSession session = createSession(sf, false, false);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session, producer);

      crash(session);

      Assert.assertTrue(session.isRollbackOnly());

      try
      {
         session.commit();

         Assert.fail("Should throw exception");
      }
      catch (HornetQException e)
      {
         Assert.assertEquals(HornetQException.TRANSACTION_ROLLED_BACK, e.getCode());
      }

      ClientMessage message = session.createMessage(false);
      int counter = RandomUtil.randomInt();
      message.putIntProperty("counter", counter);

      producer.send(message);

      // session is working again
      session.commit();

      session.start();

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();
      message = consumer.receive(1000);

      Assert.assertNotNull("expecting a message", message);
      Assert.assertEquals(counter, message.getIntProperty("counter").intValue());

      session.close();
   }

   public void testTransactedMessagesNotSentSoNoRollback() throws Exception
   {
      createSessionFactory();

      ClientSession session = createSession(sf, false, false);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session, producer);

      session.commit();

      crash(session);

      // committing again should work since didn't send anything since last commit

      Assert.assertFalse(session.isRollbackOnly());

      session.commit();

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      receiveDurableMessages(consumer);

      Assert.assertNull(consumer.receiveImmediate());

      session.commit();

      session.close();
   }

   public void testTransactedMessagesWithConsumerStartedBeforeFailover() throws Exception
   {
      createSessionFactory();

      ClientSession session = createSession(sf, false, false);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      // create a consumer and start the session before failover
      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session, producer);

      // messages will be delivered to the consumer when the session is committed
      session.commit();

      Assert.assertFalse(session.isRollbackOnly());

      crash(session);

      session.commit();

      session.close();

      session = createSession(sf, false, false);

      consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      receiveDurableMessages(consumer);

      Assert.assertNull(consumer.receiveImmediate());

      session.commit();
   }

   public void testTransactedMessagesConsumedSoRollback() throws Exception
   {
      createSessionFactory();

      ClientSession session1 = createSession(sf, false, false);

      session1.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session1.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session1, producer);

      session1.commit();

      ClientSession session2 = createSession(sf, false, false);

      ClientConsumer consumer = session2.createConsumer(FailoverTestBase.ADDRESS);

      session2.start();

      receiveMessages(consumer);

      crash(session2);

      Assert.assertTrue(session2.isRollbackOnly());

      try
      {
         session2.commit();

         Assert.fail("Should throw exception");
      }
      catch (HornetQException e)
      {
         Assert.assertEquals(HornetQException.TRANSACTION_ROLLED_BACK, e.getCode());
      }
   }

   public void testTransactedMessagesNotConsumedSoNoRollback() throws Exception
   {
      createSessionFactory();

      ClientSession session1 = createSession(sf, false, false);

      session1.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session1.createProducer(FailoverTestBase.ADDRESS);



      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         ClientMessage message = session1.createMessage(true);

         setBody(i, message);

         message.putIntProperty("counter", i);

         producer.send(message);
      }

      session1.commit();

      ClientSession session2 = createSession(sf, false, false);

      ClientConsumer consumer = session2.createConsumer(FailoverTestBase.ADDRESS);

      session2.start();

      for (int i = 0; i < NUM_MESSAGES / 2; i++)
      {
         ClientMessage message = consumer.receive(1000);

         Assert.assertNotNull(message);

         assertMessageBody(i, message);

         Assert.assertEquals(i, message.getIntProperty("counter").intValue());

         message.acknowledge();
      }

      session2.commit();

      consumer.close();

      crash(session2);

      Assert.assertFalse(session2.isRollbackOnly());

      consumer = session2.createConsumer(FailoverTestBase.ADDRESS);

      for (int i = NUM_MESSAGES / 2; i < NUM_MESSAGES; i++)
      {
         ClientMessage message = consumer.receive(1000);

         Assert.assertNotNull("expecting message " + i, message);

         assertMessageBody(i, message);

         Assert.assertEquals(i, message.getIntProperty("counter").intValue());

         message.acknowledge();
      }

      session2.commit();

      Assert.assertNull(consumer.receiveImmediate());
   }

   public void testXAMessagesSentSoRollbackOnEnd() throws Exception
   {
      createSessionFactory();

      ClientSession session = createSession(sf, true, false, false);

      Xid xid = new XidImpl("uhuhuhu".getBytes(), 126512, "auhsduashd".getBytes());

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);



      session.start(xid, XAResource.TMNOFLAGS);

      sendMessagesSomeDurable(session, producer);

      crash(session);

      try
      {
         session.end(xid, XAResource.TMSUCCESS);

         Assert.fail("Should throw exception");
      }
      catch (XAException e)
      {
         Assert.assertEquals(XAException.XA_RBOTHER, e.errorCode);
      }

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      ClientMessage message = consumer.receiveImmediate();

      Assert.assertNull(message);
   }

   public void testXAMessagesSentSoRollbackOnPrepare() throws Exception
   {
      createSessionFactory();

      final ClientSession session = createSession(sf, true, false, false);

      Xid xid = new XidImpl("uhuhuhu".getBytes(), 126512, "auhsduashd".getBytes());

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      final ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);



      session.start(xid, XAResource.TMNOFLAGS);

      sendMessagesSomeDurable(session, producer);

      session.end(xid, XAResource.TMSUCCESS);

      crash(session);

      try
      {
         session.prepare(xid);

         Assert.fail("Should throw exception");
      }
      catch (XAException e)
      {
         Assert.assertEquals(XAException.XA_RBOTHER, e.errorCode);
       // XXXX  session.rollback();
      }

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      ClientMessage message = consumer.receiveImmediate();

      Assert.assertNull(message);

      producer.close();
      consumer.close();
   }

   // This might happen if 1PC optimisation kicks in
   public void testXAMessagesSentSoRollbackOnCommit() throws Exception
   {
      createSessionFactory();

      ClientSession session = createSession(sf, true, false, false);

      Xid xid = new XidImpl("uhuhuhu".getBytes(), 126512, "auhsduashd".getBytes());

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);



      session.start(xid, XAResource.TMNOFLAGS);

      sendMessagesSomeDurable(session, producer);

      session.end(xid, XAResource.TMSUCCESS);

      crash(session);

      try
      {
         session.commit(xid, true);

         Assert.fail("Should throw exception");
      }
      catch (XAException e)
      {
         Assert.assertEquals(XAException.XAER_NOTA, e.errorCode);
      }

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      ClientMessage message = consumer.receiveImmediate();

      Assert.assertNull(message);
   }

   public void testXAMessagesNotSentSoNoRollbackOnCommit() throws Exception
   {
      createSessionFactory();

      ClientSession session = createSession(sf, true, false, false);

      Xid xid = new XidImpl("uhuhuhu".getBytes(), 126512, "auhsduashd".getBytes());

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);



      session.start(xid, XAResource.TMNOFLAGS);

      sendMessagesSomeDurable(session, producer);

      session.end(xid, XAResource.TMSUCCESS);

      session.prepare(xid);

      session.commit(xid, false);

      crash(session);

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      Xid xid2 = new XidImpl("tfytftyf".getBytes(), 54654, "iohiuohiuhgiu".getBytes());

      session.start(xid2, XAResource.TMNOFLAGS);

      receiveDurableMessages(consumer);

      session.end(xid2, XAResource.TMSUCCESS);

      session.prepare(xid2);

      session.commit(xid2, false);
   }

   public void testXAMessagesConsumedSoRollbackOnEnd() throws Exception
   {
      createSessionFactory();

      ClientSession session1 = createSession(sf, false, false);

      session1.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session1.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session1, producer);

      session1.commit();

      ClientSession session2 = createSession(sf, true, false, false);

      ClientConsumer consumer = session2.createConsumer(FailoverTestBase.ADDRESS);

      session2.start();

      Xid xid = new XidImpl("uhuhuhu".getBytes(), 126512, "auhsduashd".getBytes());

      session2.start(xid, XAResource.TMNOFLAGS);

      receiveMessages(consumer);

      crash(session2);

      try
      {
         session2.end(xid, XAResource.TMSUCCESS);

         Assert.fail("Should throw exception");
      }
      catch (XAException e)
      {
         Assert.assertEquals(XAException.XA_RBOTHER, e.errorCode);
      }
   }

   public void testXAMessagesConsumedSoRollbackOnPrepare() throws Exception
   {
      createSessionFactory();

      ClientSession session1 = createSession(sf, false, false);

      session1.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session1.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session1, producer);

      session1.commit();

      ClientSession session2 = createSession(sf, true, false, false);

      ClientConsumer consumer = session2.createConsumer(FailoverTestBase.ADDRESS);

      session2.start();

      Xid xid = new XidImpl("uhuhuhu".getBytes(), 126512, "auhsduashd".getBytes());

      session2.start(xid, XAResource.TMNOFLAGS);

      receiveMessages(consumer);

      session2.end(xid, XAResource.TMSUCCESS);

      crash(session2);

      try
      {
         session2.prepare(xid);

         Assert.fail("Should throw exception");
      }
      catch (XAException e)
      {
         Assert.assertEquals(XAException.XA_RBOTHER, e.errorCode);
      }
   }

   // 1PC optimisation
   public void testXAMessagesConsumedSoRollbackOnCommit() throws Exception
   {
      createSessionFactory();
      ClientSession session1 = createSession(sf, false, false);

      session1.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session1.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session1, producer);

      session1.commit();

      ClientSession session2 = createSession(sf, true, false, false);

      ClientConsumer consumer = session2.createConsumer(FailoverTestBase.ADDRESS);

      session2.start();

      Xid xid = new XidImpl("uhuhuhu".getBytes(), 126512, "auhsduashd".getBytes());

      session2.start(xid, XAResource.TMNOFLAGS);

      receiveMessages(consumer);

      session2.end(xid, XAResource.TMSUCCESS);

      // session2.prepare(xid);

      crash(session2);

      try
      {
         session2.commit(xid, true);

         Assert.fail("Should throw exception");
      }
      catch (XAException e)
      {
         // it should be rolled back
         Assert.assertEquals(XAException.XAER_NOTA, e.errorCode);
      }

      session1.close();

      session2.close();
   }

   public void testCreateNewFactoryAfterFailover() throws Exception
   {
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setFailoverOnInitialConnection(true);
      sf = createSessionFactoryAndWaitForTopology(locator, 2);

      ClientSession session = sendAndConsume(sf, true);

      crash(session);

      session.close();

      Thread.sleep(5000);

      createClientSessionFactory();

      session = sendAndConsume(sf, true);
   }

   public void testFailoverMultipleSessionsWithConsumers() throws Exception
   {
      createSessionFactory();

      final int numSessions = 5;

      final int numConsumersPerSession = 5;

      Map<ClientSession, List<ClientConsumer>> sessionConsumerMap = new HashMap<ClientSession, List<ClientConsumer>>();

      for (int i = 0; i < numSessions; i++)
      {
         ClientSession session = createSession(sf, true, true);

         List<ClientConsumer> consumers = new ArrayList<ClientConsumer>();

         for (int j = 0; j < numConsumersPerSession; j++)
         {
            SimpleString queueName = new SimpleString("queue" + i + "-" + j);

            session.createQueue(FailoverTestBase.ADDRESS, queueName, null, true);

            ClientConsumer consumer = session.createConsumer(queueName);

            consumers.add(consumer);
         }

         sessionConsumerMap.put(session, consumers);
      }

      ClientSession sendSession = createSession(sf, true, true);

      ClientProducer producer = sendSession.createProducer(FailoverTestBase.ADDRESS);


      sendMessages(sendSession, producer, NUM_MESSAGES);

      Set<ClientSession> sessionSet = sessionConsumerMap.keySet();
      ClientSession[] sessions = new ClientSession[sessionSet.size()];
      sessionSet.toArray(sessions);
      crash(sessions);

      for (ClientSession session : sessionConsumerMap.keySet())
      {
         session.start();
      }

      for (List<ClientConsumer> consumerList : sessionConsumerMap.values())
      {
         for (ClientConsumer consumer : consumerList)
         {
            receiveMessages(consumer);
         }
      }

      for (ClientSession session : sessionConsumerMap.keySet())
      {
         session.close();
      }
   }

   /*
    * Browser will get reset to beginning after failover
    */
   public void testFailWithBrowser() throws Exception
   {
      createSessionFactory();
      ClientSession session = createSession(sf, true, true);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session, producer);

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS, true);

      session.start();

      receiveMessages(consumer, 0, NUM_MESSAGES, false);

      crash(session);

      receiveDurableMessages(consumer);
   }

   private void sendMessagesSomeDurable(ClientSession session, ClientProducer producer) throws Exception
   {
      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         // some are durable, some are not!
         boolean durable = isDurable(i);
         ClientMessage message = session.createMessage(durable);
         setBody(i, message);
         message.putIntProperty("counter", i);
         producer.send(message);
      }
   }

   public void testFailThenReceiveMoreMessagesAfterFailover() throws Exception
   {
      createSessionFactory();

      ClientSession session = createSession(sf, true, true);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session, producer);

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      // Receive MSGs but don't ack!
      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         ClientMessage message = consumer.receive(1000);

         Assert.assertNotNull(message);

         assertMessageBody(i, message);

         Assert.assertEquals(i, message.getIntProperty("counter").intValue());
      }

      crash(session);

      // Should get the same ones after failover since we didn't ack

      receiveDurableMessages(consumer);
   }

   private void receiveDurableMessages(ClientConsumer consumer) throws HornetQException
   {
      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         // Only the persistent messages will survive

         if (isDurable(i))
         {
            ClientMessage message = consumer.receive(1000);
            Assert.assertNotNull("expecting durable msg " + i, message);
            assertMessageBody(i, message);
            Assert.assertEquals(i, message.getIntProperty("counter").intValue());
            message.acknowledge();
         }
      }
   }

   private boolean isDurable(int i)
   {
      return i % 2 == 0;
   }

   public void testFailThenReceiveMoreMessagesAfterFailover2() throws Exception
   {
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setBlockOnAcknowledge(true);
      locator.setReconnectAttempts(-1);
      sf = createSessionFactoryAndWaitForTopology(locator, 2);

      ClientSession session = createSession(sf, true, true, 0);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      sendMessagesSomeDurable(session, producer);

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();
      receiveMessages(consumer);

      crash(session);

      // Send some more

      for (int i = NUM_MESSAGES; i < NUM_MESSAGES * 2; i++)
      {
         ClientMessage message = session.createMessage(isDurable(i));

         setBody(i, message);

         message.putIntProperty("counter", i);

         producer.send(message);
      }

      receiveMessages(consumer, NUM_MESSAGES, NUM_MESSAGES * 2, true);
   }

   private void receiveMessages(ClientConsumer consumer) throws HornetQException
   {
      receiveMessages(consumer, 0, NUM_MESSAGES, true);
   }

   public void testSimpleSendAfterFailoverDurableTemporary() throws Exception
   {
      testSimpleSendAfterFailover(true, true);
   }

   public void testSimpleSendAfterFailoverNonDurableTemporary() throws Exception
   {
      testSimpleSendAfterFailover(false, true);
   }

   public void testSimpleSendAfterFailoverDurableNonTemporary() throws Exception
   {
      testSimpleSendAfterFailover(true, false);
   }

   public void testSimpleSendAfterFailoverNonDurableNonTemporary() throws Exception
   {
      testSimpleSendAfterFailover(false, false);
   }

   private void testSimpleSendAfterFailover(final boolean durable, final boolean temporary) throws Exception
   {
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setBlockOnAcknowledge(true);
      locator.setReconnectAttempts(-1);
      sf = createSessionFactoryAndWaitForTopology(locator, 2);

      ClientSession session = createSession(sf, true, true, 0);

      if (temporary)
      {
         session.createTemporaryQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null);
      }
      else
      {
         session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, durable);
      }

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      crash(session);

      sendMessagesSomeDurable(session, producer);

      receiveMessages(consumer);
   }

   public void _testForceBlockingReturn() throws Exception
   {
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setBlockOnAcknowledge(true);
      locator.setReconnectAttempts(-1);
      createClientSessionFactory();

      // Add an interceptor to delay the send method so we can get time to cause failover before it returns

      // liveServer.getRemotingService().addInterceptor(new DelayInterceptor());

      final ClientSession session = createSession(sf, true, true, 0);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      final ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      class Sender extends Thread
      {
         @Override
         public void run()
         {
            ClientMessage message = session.createMessage(true);

            message.getBodyBuffer().writeString("message");

            try
            {
               producer.send(message);
            }
            catch (HornetQException e)
            {
               this.e = e;
            }
         }

         volatile HornetQException e;
      }

      Sender sender = new Sender();

      sender.start();

      Thread.sleep(500);

      crash(session);

      sender.join();

      Assert.assertNotNull(sender.e);

      Assert.assertEquals(sender.e.getCode(), HornetQException.UNBLOCKED);

      session.close();
   }

   public void testCommitOccurredUnblockedAndResendNoDuplicates() throws Exception
   {
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setReconnectAttempts(-1);

      locator.setBlockOnAcknowledge(true);

      sf = createSessionFactoryAndWaitForTopology(locator, 2);

      final ClientSession session = createSession(sf, false, false);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      String txID = "my-tx-id";

      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         ClientMessage message = session.createMessage(true);

         if (i == 0)
         {
            // Only need to add it on one message per tx
            message.putStringProperty(Message.HDR_DUPLICATE_DETECTION_ID, new SimpleString(txID));
         }

         setBody(i, message);

         message.putIntProperty("counter", i);

         producer.send(message);
      }

      class Committer extends Thread
      {
         DelayInterceptor2 interceptor = new DelayInterceptor2();

         @Override
         public void run()
         {
            try
            {
               sf.getServerLocator().addInterceptor(interceptor);

               session.commit();
            }
            catch (HornetQException e)
            {
               if (e.getCode() == HornetQException.TRANSACTION_ROLLED_BACK || e.getCode() == HornetQException.TRANSACTION_OUTCOME_UNKNOWN)
               {
                  // Ok - now we retry the commit after removing the interceptor

                  sf.getServerLocator().removeInterceptor(interceptor);

                  try
                  {
                     session.commit();

                     failed = false;
                  }
                  catch (HornetQException e2)
                  {
                     throw new RuntimeException(e2);
                  }

               }
            }
         }

         volatile boolean failed = true;
      }

      Committer committer = new Committer();

      // Commit will occur, but response will never get back, connection is failed, and commit
      // should be unblocked with transaction rolled back

      committer.start();

      // Wait for the commit to occur and the response to be discarded
      assertTrue(committer.interceptor.await());

      Thread.sleep(500);

      crash(session);

      committer.join();

      Assert.assertFalse("second attempt succeed?", committer.failed);

      session.close();

      ClientSession session2 = createSession(sf, false, false);

      producer = session2.createProducer(FailoverTestBase.ADDRESS);

      // We now try and resend the messages since we get a transaction rolled back exception
      // but the commit actually succeeded, duplicate detection should kick in and prevent dups

      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         ClientMessage message = session2.createMessage(true);

         if (i == 0)
         {
            // Only need to add it on one message per tx
            message.putStringProperty(Message.HDR_DUPLICATE_DETECTION_ID, new SimpleString(txID));
         }

         setBody(i, message);

         message.putIntProperty("counter", i);

         producer.send(message);
      }

      try
      {
         session2.commit();
         fail("expecting DUPLICATE_ID_REJECTED exception");
      }
      catch (HornetQException e)
      {
         assertEquals(e.getMessage(), HornetQException.DUPLICATE_ID_REJECTED, e.getCode());
      }

      ClientConsumer consumer = session2.createConsumer(FailoverTestBase.ADDRESS);

      session2.start();

      receiveMessages(consumer);

      ClientMessage message = consumer.receiveImmediate();

      Assert.assertNull(message);
   }

   public void testCommitDidNotOccurUnblockedAndResend() throws Exception
   {
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setBlockOnAcknowledge(true);
      locator.setReconnectAttempts(-1);
      sf = createSessionFactoryAndWaitForTopology(locator, 2);

      final ClientSession session = createSession(sf, false, false);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);



      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);
      sendMessages(session, producer,NUM_MESSAGES);

      class Committer extends Thread
      {
         @Override
         public void run()
         {
            Interceptor interceptor = new DelayInterceptor3();

            try
            {
               liveServer.addInterceptor(interceptor);

               session.commit();
            }
            catch (HornetQException e)
            {
               if (e.getCode() == HornetQException.TRANSACTION_ROLLED_BACK || e.getCode() == HornetQException.TRANSACTION_OUTCOME_UNKNOWN)
               {
                  // Ok - now we retry the commit after removing the interceptor

                  liveServer.removeInterceptor(interceptor);

                  try
                  {
                     session.commit();

                     failed = false;
                  }
                  catch (HornetQException e2)
                  {
                  }
               }
            }
         }

         volatile boolean failed = true;
      }

      Committer committer = new Committer();

      committer.start();

      Thread.sleep(500);

      crash(session);

      committer.join();

      Assert.assertFalse(committer.failed);

      session.close();

      ClientSession session2 = createSession(sf, false, false);

      producer = session2.createProducer(FailoverTestBase.ADDRESS);

      // We now try and resend the messages since we get a transaction rolled back exception
      sendMessages(session2, producer,NUM_MESSAGES);

      session2.commit();

      ClientConsumer consumer = session2.createConsumer(FailoverTestBase.ADDRESS);

      session2.start();

      receiveMessages(consumer);

      ClientMessage message = consumer.receiveImmediate();

      Assert.assertNull("expecting null message", message);
   }

   public void testBackupServerNotRemoved() throws Exception
   {
      // HORNETQ-720 Disabling test for replicating backups.
      if (!backupServer.getServer().getConfiguration().isSharedStore())
      {
         waitForComponent(backupServer, 1);
         return;
      }
      locator.setFailoverOnInitialConnection(true);
      createSessionFactory();

      CountDownSessionFailureListener listener = new CountDownSessionFailureListener();
      ClientSession session = sendAndConsume(sf, true);

      session.addFailureListener(listener);

      backupServer.stop();

      liveServer.crash();

      // To reload security or other settings that are read during startup
      beforeRestart(backupServer);

      backupServer.start();

      assertTrue("session failure listener", listener.getLatch().await(5, TimeUnit.SECONDS));

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      ClientMessage message = session.createMessage(true);

      setBody(0, message);

      producer.send(message);
   }

   public void testLiveAndBackupLiveComesBack() throws Exception
   {
      locator.setFailoverOnInitialConnection(true);
      createSessionFactory();
      final CountDownLatch latch = new CountDownLatch(1);

      ClientSession session = sendAndConsume(sf, true);

      session.addFailureListener(new CountDownSessionFailureListener(latch));

      backupServer.stop();

      liveServer.crash();

      beforeRestart(liveServer);

      // To reload security or other settings that are read during startup
      beforeRestart(liveServer);

      liveServer.start();

      assertTrue(latch.await(5, TimeUnit.SECONDS));

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      ClientMessage message = session.createMessage(true);

      setBody(0, message);

      producer.send(message);
   }

   public void testLiveAndBackupLiveComesBackNewFactory() throws Exception
   {
      locator.setFailoverOnInitialConnection(true);
      createSessionFactory();

      final CountDownLatch latch = new CountDownLatch(1);

      ClientSession session = sendAndConsume(sf, true);

      session.addFailureListener(new CountDownSessionFailureListener(latch));

      backupServer.stop();

      liveServer.crash();

      // To reload security or other settings that are read during startup
      beforeRestart(liveServer);

      liveServer.start();

      assertTrue(latch.await(5, TimeUnit.SECONDS));

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      ClientMessage message = session.createMessage(true);

      setBody(0, message);

      producer.send(message);

      session.close();

      sf.close();

      createClientSessionFactory();

      session = createSession(sf);

      ClientConsumer cc = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      ClientMessage cm = cc.receive(5000);

      assertNotNull(cm);

      Assert.assertEquals("message0", cm.getBodyBuffer().readString());
   }

   public void testLiveAndBackupBackupComesBackNewFactory() throws Exception
   {
      locator.setBlockOnNonDurableSend(true);
      locator.setBlockOnDurableSend(true);
      locator.setFailoverOnInitialConnection(true);
      locator.setReconnectAttempts(-1);
      sf = createSessionFactoryAndWaitForTopology(locator, 2);
      CountDownSessionFailureListener listener = new CountDownSessionFailureListener();

      ClientSession session = sendAndConsume(sf, true);

      session.addFailureListener(listener);

      backupServer.stop();

      liveServer.crash();

      // To reload security or other settings that are read during startup
      beforeRestart(backupServer);

      if (!backupServer.getServer().getConfiguration().isSharedStore())
      {
    	  // XXX
         // this test would not make sense in the remote replication use case, without the following
         backupServer.getServer().getConfiguration().setBackup(false);
      }

      backupServer.start();

      assertTrue("session failure listener", listener.getLatch().await(5, TimeUnit.SECONDS));

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      ClientMessage message = session.createMessage(true);

      setBody(0, message);

      producer.send(message);

      session.close();

      sf.close();

      createClientSessionFactory();

      session = createSession(sf);

      ClientConsumer cc = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      ClientMessage cm = cc.receive(5000);

      assertNotNull(cm);

      Assert.assertEquals("message0", cm.getBodyBuffer().readString());
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   @Override
   protected TransportConfiguration getAcceptorTransportConfiguration(final boolean live)
   {
      return TransportConfigurationUtils.getInVMAcceptor(live);
   }

   @Override
   protected TransportConfiguration getConnectorTransportConfiguration(final boolean live)
   {
      return TransportConfigurationUtils.getInVMConnector(live);
   }

   protected void beforeRestart(TestableServer liveServer)
   {
   }

   // Private -------------------------------------------------------

   private ClientSession sendAndConsume(final ClientSessionFactory sf, final boolean createQueue) throws Exception
   {
      ClientSession session = createSession(sf, false, true, true);

      if (createQueue)
      {
         session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, false);
      }

      ClientProducer producer = session.createProducer(FailoverTestBase.ADDRESS);

      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         ClientMessage message = session.createMessage(HornetQTextMessage.TYPE,
                                                       false,
                                                       0,
                                                       System.currentTimeMillis(),
                                                       (byte)1);
         message.putIntProperty(new SimpleString("count"), i);
         message.getBodyBuffer().writeString("aardvarks");
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);

      session.start();

      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         ClientMessage message2 = consumer.receive();

         Assert.assertEquals("aardvarks", message2.getBodyBuffer().readString());

         Assert.assertEquals(i, message2.getObjectProperty(new SimpleString("count")));

         message2.acknowledge();
      }

      ClientMessage message3 = consumer.receiveImmediate();

      Assert.assertNull(message3);

      return session;
   }

   // Inner classes -------------------------------------------------

}
