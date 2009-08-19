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

package org.hornetq.core.transaction;

import java.util.Collection;

import org.hornetq.core.server.Queue;

/**
 * 
 * A TransactionOperation
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public interface TransactionOperation
{
   
   /** rollback will need a distinct list of Queues in order to lock those queues before calling rollback */
   Collection<Queue> getDistinctQueues();
   
   void beforePrepare(Transaction tx) throws Exception;
   
   void beforeCommit(Transaction tx) throws Exception;
   
   void beforeRollback(Transaction tx) throws Exception;
   
   void afterPrepare(Transaction tx) throws Exception;
      
   void afterCommit(Transaction tx) throws Exception;
   
   void afterRollback(Transaction tx) throws Exception;   
}