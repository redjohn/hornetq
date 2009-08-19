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

package org.hornetq.tests.unit.core.deployers.impl;

import org.hornetq.core.deployers.DeploymentManager;
import org.hornetq.core.deployers.impl.AddressSettingsDeployer;
import org.hornetq.core.settings.HierarchicalRepository;
import org.hornetq.core.settings.impl.AddressSettings;
import org.hornetq.core.settings.impl.HierarchicalObjectRepository;
import org.hornetq.tests.util.UnitTestCase;
import org.hornetq.utils.SimpleString;
import org.hornetq.utils.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 */
public class AddressSettingsDeployerTest extends UnitTestCase
{
   private String conf = "<address-settings match=\"queues.*\">\n" +
           "      <dead-letter-address>DLQtest</dead-letter-address>\n" +
           "      <expiry-address>ExpiryQueueTest</expiry-address>\n" +
           "      <redelivery-delay>100</redelivery-delay>\n" +
           "      <max-size-bytes>-100</max-size-bytes>\n" +
           "      <distribution-policy-class>org.hornetq.core.impl.RoundRobinDistributionPolicy</distribution-policy-class>\n" +
           "      <message-counter-history-day-limit>1000</message-counter-history-day-limit>\n" +
           "   </address-settings>";

   private AddressSettingsDeployer addressSettingsDeployer;

   private HierarchicalRepository<AddressSettings> repository;

   protected void setUp() throws Exception
   {
      super.setUp();
      
      repository = new HierarchicalObjectRepository<AddressSettings>();
      DeploymentManager deploymentManager = new FakeDeploymentManager();
      addressSettingsDeployer = new AddressSettingsDeployer(deploymentManager, repository);
   }

   public void testDeploy() throws Exception
   {
      addressSettingsDeployer.deploy(XMLUtil.stringToElement(conf));
      AddressSettings as = repository.getMatch("queues.aq");
      assertNotNull(as);
      assertEquals(100, as.getRedeliveryDelay());
      assertEquals(-100, as.getMaxSizeBytes());
      assertEquals("org.hornetq.core.impl.RoundRobinDistributionPolicy", as.getDistributionPolicyClass());
      assertEquals(1000, as.getMessageCounterHistoryDayLimit());
      assertEquals(new SimpleString("DLQtest"), as.getDeadLetterAddress());
      assertEquals(new SimpleString("ExpiryQueueTest"), as.getExpiryAddress());
   }
   
   public void testDeployFromConfigurationFile() throws Exception
   {
      String xml = "<configuration xmlns='urn:jboss:messaging'> " 
                 + "<address-settings>"
                 + "   <address-setting match=\"queues.*\">"
                 + "      <dead-letter-address>DLQtest</dead-letter-address>\n"
                 + "      <expiry-address>ExpiryQueueTest</expiry-address>\n"
                 + "      <redelivery-delay>100</redelivery-delay>\n"
                 + "      <max-size-bytes>-100</max-size-bytes>\n"
                 + "      <distribution-policy-class>org.hornetq.core.impl.RoundRobinDistributionPolicy</distribution-policy-class>"
                 + "      <message-counter-history-day-limit>1000</message-counter-history-day-limit>"
                 + "   </address-setting>"
                 + "</address-settings>"
                 + "</configuration>";
      
      Element rootNode = org.hornetq.utils.XMLUtil.stringToElement(xml);
      addressSettingsDeployer.validate(rootNode);
      NodeList addressSettingsNode = rootNode.getElementsByTagName("address-setting");
      assertEquals(1, addressSettingsNode.getLength());

      addressSettingsDeployer.deploy(addressSettingsNode.item(0));
      AddressSettings as = repository.getMatch("queues.aq");
      assertNotNull(as);
      assertEquals(100, as.getRedeliveryDelay());
      assertEquals(-100, as.getMaxSizeBytes());
      assertEquals("org.hornetq.core.impl.RoundRobinDistributionPolicy", as.getDistributionPolicyClass());
      assertEquals(1000, as.getMessageCounterHistoryDayLimit());
      assertEquals(new SimpleString("DLQtest"), as.getDeadLetterAddress());
      assertEquals(new SimpleString("ExpiryQueueTest"), as.getExpiryAddress());
   }

   public void testUndeploy() throws Exception
   {
      addressSettingsDeployer.deploy(XMLUtil.stringToElement(conf));
      AddressSettings as = repository.getMatch("queues.aq");
      assertNotNull(as);
      addressSettingsDeployer.undeploy(XMLUtil.stringToElement(conf));
      as = repository.getMatch("queues.aq");
      assertNull(as);
   }

}