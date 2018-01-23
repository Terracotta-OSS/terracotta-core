/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

package com.tc.config.schema.setup;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author vmad
 */
public class ClientConfigurationSetupManagerFactoryTest {

  public static final String HOST = "localhost";
  public static final int PORT = 1234;

  @Test
  public void testGetL1TVSConfigurationSetupManager() throws Exception {
    ClientConfigurationSetupManagerFactory clientConfigurationSetupManagerFactory = new ClientConfigurationSetupManagerFactory(new String[0], Collections.singletonList(InetSocketAddress.createUnresolved(HOST, PORT)));
    L1ConfigurationSetupManager l1ConfigurationSetupManager = clientConfigurationSetupManagerFactory.getL1TVSConfigurationSetupManager();

    Assert.assertNotNull(l1ConfigurationSetupManager);
    Assert.assertEquals(HOST, l1ConfigurationSetupManager.l2Config().l2Data()[0].host());
    Assert.assertEquals(PORT, l1ConfigurationSetupManager.l2Config().l2Data()[0].tsaPort());
  }

  @Test
  public void testGetL1TVSConfigurationSetupManagerMultiUrl() throws Exception {
    List<InetSocketAddress> stripeMemberUris = new Vector<>();
    stripeMemberUris.add(InetSocketAddress.createUnresolved(HOST, PORT));
    stripeMemberUris.add(InetSocketAddress.createUnresolved(HOST, PORT));
    ClientConfigurationSetupManagerFactory clientConfigurationSetupManagerFactory = new ClientConfigurationSetupManagerFactory(new String[0], stripeMemberUris);
    L1ConfigurationSetupManager l1ConfigurationSetupManager = clientConfigurationSetupManagerFactory.getL1TVSConfigurationSetupManager();

    Assert.assertNotNull(l1ConfigurationSetupManager);
    Assert.assertEquals(HOST, l1ConfigurationSetupManager.l2Config().l2Data()[0].host());
    Assert.assertEquals(PORT, l1ConfigurationSetupManager.l2Config().l2Data()[0].tsaPort());

    Assert.assertEquals(HOST, l1ConfigurationSetupManager.l2Config().l2Data()[1].host());
    Assert.assertEquals(PORT, l1ConfigurationSetupManager.l2Config().l2Data()[1].tsaPort());
  }

  @Test (expected = IllegalArgumentException.class)
  public void testGetL1TVSConfigurationSetupManagerFailure() throws Exception {
    ClientConfigurationSetupManagerFactory clientConfigurationSetupManagerFactory = new ClientConfigurationSetupManagerFactory(new String[0], Collections.singletonList(InetSocketAddress.createUnresolved("local", -1)));
    clientConfigurationSetupManagerFactory.getL1TVSConfigurationSetupManager();
  }
}