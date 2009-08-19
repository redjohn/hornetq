/*
 * JBoss, Home of Professional Open Source Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors by
 * the @authors tag. See the copyright.txt in the distribution for a full listing of individual contributors. This is
 * free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. You should have received a copy of the GNU Lesser General Public License along with this software; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.hornetq.core.remoting.impl.wireformat.replication;

import org.hornetq.core.remoting.impl.wireformat.PacketImpl;
import org.hornetq.core.remoting.spi.MessagingBuffer;
import org.hornetq.utils.SimpleString;
import org.hornetq.utils.TypedProperties;

/**
 * 
 * A ReplicateRemoteConsumerAddedMessage
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Created 4 Mar 2009 18:36:30
 *
 *
 */
public class ReplicateRemoteConsumerAddedMessage extends PacketImpl
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private SimpleString uniqueBindingName;

   private SimpleString filterString;

   private TypedProperties properties;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public ReplicateRemoteConsumerAddedMessage(final SimpleString uniqueBindingName,
                                              final SimpleString filterString,
                                              final TypedProperties properties)
   {
      super(REPLICATE_ADD_REMOTE_CONSUMER);

      this.uniqueBindingName = uniqueBindingName;

      this.filterString = filterString;

      this.properties = properties;
   }

   // Public --------------------------------------------------------

   public ReplicateRemoteConsumerAddedMessage()
   {
      super(REPLICATE_ADD_REMOTE_CONSUMER);
   }

   public int getRequiredBufferSize()
   {
      return BASIC_PACKET_SIZE + 
             uniqueBindingName.sizeof() + // buffer.writeSimpleString(uniqueBindingName);
             SimpleString.sizeofNullableString(filterString) + // buffer.writeNullableSimpleString(filterString);
             properties.getEncodeSize(); // properties.encode(buffer);
   }

   @Override
   public void encodeBody(final MessagingBuffer buffer)
   {
      buffer.writeSimpleString(uniqueBindingName);

      buffer.writeNullableSimpleString(filterString);

      properties.encode(buffer);
   }

   @Override
   public void decodeBody(final MessagingBuffer buffer)
   {
      uniqueBindingName = buffer.readSimpleString();

      filterString = buffer.readNullableSimpleString();

      properties = new TypedProperties();

      properties.decode(buffer);
   }

   public SimpleString getUniqueBindingName()
   {
      return uniqueBindingName;
   }

   public SimpleString getFilterString()
   {
      return filterString;
   }

   public TypedProperties getProperties()
   {
      return properties;
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}