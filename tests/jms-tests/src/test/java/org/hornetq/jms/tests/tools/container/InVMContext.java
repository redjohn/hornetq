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

package org.hornetq.jms.tests.tools.container;

import org.hornetq.jms.tests.JmsTestLogger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;

/**
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 2868 $</tt>
 *
 * $Id: InVMContext.java 2868 2007-07-10 20:22:16Z timfox $
 */
public class InVMContext implements Context, Serializable
{
   // Constants -----------------------------------------------------

   private static final long serialVersionUID = 385743957345L;

   private static final JmsTestLogger log = JmsTestLogger.LOGGER;

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   protected Map map;

   protected NameParser parser = new InVMNameParser();

   private String nameInNamespace = "";

   // Constructors --------------------------------------------------

   public InVMContext()
   {
      map = Collections.synchronizedMap(new HashMap());
   }

   public InVMContext(final String nameInNamespace)
   {
      this();
      this.nameInNamespace = nameInNamespace;
   }

   // Context implementation ----------------------------------------

   public Object lookup(final Name name) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public Object lookup(String name) throws NamingException
   {
      name = trimSlashes(name);
      int i = name.indexOf("/");
      String tok = i == -1 ? name : name.substring(0, i);
      Object value = map.get(tok);
      if (value == null)
      {
         throw new NameNotFoundException("Name not found: " + tok);
      }
      if (value instanceof InVMContext && i != -1)
      {
         return ((InVMContext)value).lookup(name.substring(i));
      }
      if (value instanceof Reference)
      {
         Reference ref = (Reference)value;
         RefAddr refAddr = ref.get("nns");

         // we only deal with references create by NonSerializableFactory
         String key = (String)refAddr.getContent();
         return NonSerializableFactory.lookup(key);
      }
      else
      {
         return value;
      }
   }

   public void bind(final Name name, final Object obj) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public void bind(final String name, final Object obj) throws NamingException
   {
      internalBind(name, obj, false);
   }

   public void rebind(final Name name, final Object obj) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public void rebind(final String name, final Object obj) throws NamingException
   {
      internalBind(name, obj, true);
   }

   public void unbind(final Name name) throws NamingException
   {
      unbind(name.toString());
   }

   public void unbind(String name) throws NamingException
   {
      name = trimSlashes(name);
      int i = name.indexOf("/");
      boolean terminal = i == -1;
      if (terminal)
      {
         map.remove(name);
      }
      else
      {
         String tok = name.substring(0, i);
         InVMContext c = (InVMContext)map.get(tok);
         if (c == null)
         {
            throw new NameNotFoundException("Context not found: " + tok);
         }
         c.unbind(name.substring(i));
      }
   }

   public void rename(final Name oldName, final Name newName) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public void rename(final String oldName, final String newName) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public NamingEnumeration list(final Name name) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public NamingEnumeration list(final String name) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public NamingEnumeration listBindings(final Name name) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public NamingEnumeration listBindings(String contextName) throws NamingException
   {
      contextName = trimSlashes(contextName);
      if (!"".equals(contextName) && !".".equals(contextName))
      {
         try
         {
            return ((InVMContext)lookup(contextName)).listBindings("");
         }
         catch (Throwable t)
         {
            throw new NamingException(t.getMessage());
         }
      }

      List l = new ArrayList();
      for (Iterator i = map.keySet().iterator(); i.hasNext();)
      {
         String name = (String)i.next();
         Object object = map.get(name);
         l.add(new Binding(name, object));
      }
      return new NamingEnumerationImpl(l.iterator());
   }

   public void destroySubcontext(final Name name) throws NamingException
   {
      destroySubcontext(name.toString());
   }

   public void destroySubcontext(final String name) throws NamingException
   {
      map.remove(trimSlashes(name));
   }

   public Context createSubcontext(final Name name) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public Context createSubcontext(String name) throws NamingException
   {
      name = trimSlashes(name);
      if (map.get(name) != null)
      {
         throw new NameAlreadyBoundException(name);
      }
      InVMContext c = new InVMContext(getNameInNamespace());
      map.put(name, c);
      return c;
   }

   public Object lookupLink(final Name name) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public Object lookupLink(final String name) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public NameParser getNameParser(final Name name) throws NamingException
   {
      return getNameParser(name.toString());
   }

   public NameParser getNameParser(final String name) throws NamingException
   {
      return parser;
   }

   public Name composeName(final Name name, final Name prefix) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public String composeName(final String name, final String prefix) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public Object addToEnvironment(final String propName, final Object propVal) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public Object removeFromEnvironment(final String propName) throws NamingException
   {
      throw new UnsupportedOperationException();
   }

   public Hashtable getEnvironment() throws NamingException
   {
      Hashtable env = new Hashtable();
      env.put("java.naming.factory.initial", "org.hornetq.jms.tests.tools.container.InVMInitialContextFactory");
      env.put("java.naming.provider.url", "org.jboss.naming:org.jnp.interface");
      return env;
   }

   public void close() throws NamingException
   {
   }

   public String getNameInNamespace() throws NamingException
   {
      return nameInNamespace;
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   private String trimSlashes(String s)
   {
      int i = 0;
      while (true)
      {
         if (i == s.length() || s.charAt(i) != '/')
         {
            break;
         }
         i++;
      }
      s = s.substring(i);
      i = s.length() - 1;
      while (true)
      {
         if (i == -1 || s.charAt(i) != '/')
         {
            break;
         }
         i--;
      }
      return s.substring(0, i + 1);
   }

   private void internalBind(String name, final Object obj, final boolean rebind) throws NamingException
   {
      InVMContext.log.debug("Binding " + name + " obj " + obj + " rebind " + rebind);
      name = trimSlashes(name);
      int i = name.lastIndexOf("/");
      InVMContext c = this;
      if (i != -1)
      {
         String path = name.substring(0, i);
         c = (InVMContext)lookup(path);
      }
      name = name.substring(i + 1);
      if (!rebind && c.map.get(name) != null)
      {
         throw new NameAlreadyBoundException(name);
      }
      c.map.put(name, obj);
   }

   // Inner classes -------------------------------------------------

   private class NamingEnumerationImpl implements NamingEnumeration
   {
      private final Iterator iterator;

      NamingEnumerationImpl(final Iterator bindingIterator)
      {
         iterator = bindingIterator;
      }

      public void close() throws NamingException
      {
         throw new UnsupportedOperationException();
      }

      public boolean hasMore() throws NamingException
      {
         return iterator.hasNext();
      }

      public Object next() throws NamingException
      {
         return iterator.next();
      }

      public boolean hasMoreElements()
      {
         return iterator.hasNext();
      }

      public Object nextElement()
      {
         return iterator.next();
      }
   }
}
