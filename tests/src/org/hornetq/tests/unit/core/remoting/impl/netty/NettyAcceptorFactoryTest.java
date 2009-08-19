/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors
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
package org.hornetq.tests.unit.core.remoting.impl.netty;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.exception.MessagingException;
import org.hornetq.core.remoting.impl.AbstractBufferHandler;
import org.hornetq.core.remoting.spi.Acceptor;
import org.hornetq.core.remoting.spi.BufferHandler;
import org.hornetq.core.remoting.spi.Connection;
import org.hornetq.core.remoting.spi.ConnectionLifeCycleListener;
import org.hornetq.core.remoting.spi.MessagingBuffer;
import org.hornetq.integration.transports.netty.NettyAcceptor;
import org.hornetq.integration.transports.netty.NettyAcceptorFactory;
import org.hornetq.tests.util.UnitTestCase;

/**
 *
 * A NettyAcceptorFactoryTest
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class NettyAcceptorFactoryTest extends UnitTestCase
{
   public void testCreateAcceptor() throws Exception
   {
      NettyAcceptorFactory factory = new NettyAcceptorFactory();

      Map<String, Object> params = new HashMap<String, Object>();
      BufferHandler handler = new AbstractBufferHandler()
      {
         
         public void bufferReceived(Object connectionID, MessagingBuffer buffer)
         {
         }
      };
      
      ConnectionLifeCycleListener listener = new ConnectionLifeCycleListener()
      {
         
         public void connectionException(Object connectionID, MessagingException me)
         {
         }
         
         public void connectionDestroyed(Object connectionID)
         {
         }
         
         public void connectionCreated(Connection connection)
         {
         }
      };
           
      Acceptor acceptor = factory.createAcceptor(params, handler, listener, 
                                                 Executors.newCachedThreadPool(),
                                                 Executors.newScheduledThreadPool(ConfigurationImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE));

      assertTrue(acceptor instanceof NettyAcceptor);
   }
}