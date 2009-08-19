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

package org.hornetq.core.management.jmx.impl;

import javax.management.StandardMBean;

import org.hornetq.core.logging.Logger;
import org.hornetq.core.management.ReplicationOperationInvoker;

/**
 * A ReplicationAwareStandardMBeanWrapper
 *
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 * Created Dec 3, 2008 11:23:11
 *
 *
 */
public class ReplicationAwareStandardMBeanWrapper extends StandardMBean
{
   // Constants -----------------------------------------------------
   
   private static final Logger log = Logger.getLogger(ReplicationAwareStandardMBeanWrapper.class);


   // Attributes ----------------------------------------------------

   private final String resourceName;

   private final ReplicationOperationInvoker replicationInvoker;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   protected ReplicationAwareStandardMBeanWrapper(final String resourceName,
                                                  final Class mbeanInterface,
                                                  final ReplicationOperationInvoker replicationInvoker) throws Exception
   {
      super(mbeanInterface);

      this.resourceName = resourceName;
      
      this.replicationInvoker = replicationInvoker;
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   protected Object replicationAwareInvoke(final String operationName, final Object... parameters) throws Exception
   {
      return replicationInvoker.invoke(resourceName, operationName, parameters);
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}