/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.hornetq.tests.integration.cluster.failover;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.hornetq.core.client.ClientMessage;
import org.hornetq.core.client.impl.ClientSessionFactoryInternal;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.TransportConfiguration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnection;
import org.hornetq.core.remoting.impl.invm.InVMRegistry;
import org.hornetq.core.remoting.impl.invm.TransportConstants;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.server.Messaging;

/**
 * A LargeMessageMultiThreadFailoverTest
 *
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * 
 * Created Jan 18, 2009 4:52:09 PM
 *
 *
 */
public class LargeMessageMultiThreadFailoverTest extends MultiThreadRandomFailoverTestBase
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------
   private final byte[] FIVE_HUNDRED_BYTES = new byte[500];

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   protected int getLatchWait()
   {
      return 60000;
   }
   
   protected ClientSessionFactoryInternal createSessionFactory()
   {
      ClientSessionFactoryInternal sf = super.createSessionFactory();

      sf.setMinLargeMessageSize(200);

      return sf;

   }

   protected void start() throws Exception
   {
      InVMRegistry.instance.clear();

      startJournal();
   }
   
   protected void startJournal() throws Exception
   {
      deleteDirectory(new File(getTestDir()));

      Configuration backupConf = new ConfigurationImpl();

      backupConf.setJournalDirectory(getJournalDir(getTestDir() + "/backup"));
      backupConf.setLargeMessagesDirectory(getLargeMessagesDir(getTestDir() + "/backup"));
      backupConf.setBindingsDirectory(getBindingsDir(getTestDir() + "/backup"));
      backupConf.setPagingDirectory(getPageDir(getTestDir() + "/backup"));
      backupConf.setJournalFileSize(100 * 1024);
      
      backupConf.setJournalType(JournalType.ASYNCIO);

      backupConf.setSecurityEnabled(false);
      backupParams.put(TransportConstants.SERVER_ID_PROP_NAME, 1);

      backupConf.getAcceptorConfigurations()
                .add(new TransportConfiguration(InVMAcceptorFactory.class.getCanonicalName(), backupParams));
      backupConf.setBackup(true);

      backupServer = Messaging.newMessagingServer(backupConf);
      backupServer.start();

      Configuration liveConf = new ConfigurationImpl();

      liveConf.setJournalDirectory(getJournalDir(getTestDir() + "/live"));
      liveConf.setLargeMessagesDirectory(getLargeMessagesDir(getTestDir() + "/live"));
      liveConf.setBindingsDirectory(getBindingsDir(getTestDir() + "/live"));
      liveConf.setPagingDirectory(getPageDir(getTestDir() + "/live"));

      liveConf.setJournalFileSize(100 * 1024);
      
      liveConf.setJournalType(JournalType.ASYNCIO);

      liveConf.setSecurityEnabled(false);
      liveConf.getAcceptorConfigurations()
              .add(new TransportConfiguration(InVMAcceptorFactory.class.getCanonicalName()));

      Map<String, TransportConfiguration> connectors = new HashMap<String, TransportConfiguration>();

      TransportConfiguration backupTC = new TransportConfiguration(INVM_CONNECTOR_FACTORY,
                                                                   backupParams,
                                                                   "backup-connector");
      connectors.put(backupTC.getName(), backupTC);
      liveConf.setConnectorConfigurations(connectors);
      liveConf.setBackupConnectorName(backupTC.getName());
      liveServer = Messaging.newMessagingServer(liveConf);

      liveServer.start();

   }  

   @Override
   protected void setBody(final ClientMessage message) throws Exception
   {
      message.getBody().writeBytes(FIVE_HUNDRED_BYTES);

   }

   /* (non-Javadoc)
    * @see org.hornetq.tests.integration.cluster.failover.MultiThreadRandomFailoverTestBase#checkSize(org.hornetq.core.client.ClientMessage)
    */
   @Override
   protected boolean checkSize(ClientMessage message)
   {
      return 500 == message.getBodySize();
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}