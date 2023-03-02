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
package com.tc.config;

import org.junit.Test;

import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import org.terracotta.configuration.ServerConfiguration;
import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroupConfigurationTest {

  private final String[] SERVER_NAMES = {"server-1", "server-2"};
  private final int[]    SERVER_PORTS = {1000, 2000};
  private final Node[]   NODES = new Node[SERVER_NAMES.length];

  private final List<ServerConfiguration> serverConfigurationMap = new ArrayList<>();

  @Test
  public void testGetCurrentNode() {
    createServiceConfigurations();
    GroupConfiguration groupConfiguration = new GroupConfiguration(serverConfigurationMap, SERVER_NAMES[0]);

    assertThat(groupConfiguration.getCurrentNode(), is(NODES[0]));
  }

  @Test
  public void testGetNodes() {
    createServiceConfigurations();
    GroupConfiguration groupConfiguration = new GroupConfiguration(serverConfigurationMap, SERVER_NAMES[0]);

    assertThat(groupConfiguration.getNodes(), containsInAnyOrder(NODES));
  }

  @Test
  public void testGetMembers() {
    createServiceConfigurations();
    GroupConfiguration groupConfiguration = new GroupConfiguration(serverConfigurationMap, SERVER_NAMES[0]);

    assertThat(groupConfiguration.getMembers(), arrayContainingInAnyOrder(SERVER_NAMES));
  }

  @Test
  public void testGetElectionTimeInSecsWithMultipleServers() {
    createServiceConfigurations();
    GroupConfiguration groupConfiguration = new GroupConfiguration(serverConfigurationMap, SERVER_NAMES[0]);

    assertThat(groupConfiguration.getElectionTimeInSecs(), is(GroupConfiguration.MULTI_SERVER_ELECTION_TIMEOUT));
  }

  @Test
  public void testGetElectionTimeInSecsWithSingleServer() {
    createServiceConfigurations();
    serverConfigurationMap.remove(1);
    GroupConfiguration groupConfiguration = new GroupConfiguration(serverConfigurationMap, SERVER_NAMES[0]);

    assertThat(groupConfiguration.getElectionTimeInSecs(), is(GroupConfiguration.SINGLE_SERVER_ELECTION_TIMEOUT));
  }

  @Test
  public void testGetNodesWithWildCard() {
    createServiceConfigurations(true);
    GroupConfiguration groupConfiguration = new GroupConfiguration(serverConfigurationMap, SERVER_NAMES[0]);

    assertThat(groupConfiguration.getNodes(), containsInAnyOrder(NODES));
  }

  private void createServiceConfigurations() {
    createServiceConfigurations(false);
  }

  private void createServiceConfigurations(boolean wildcard) {
    String hostName = wildcard ? TCSocketAddress.WILDCARD_IP : "localhost";

    for (int i = 0; i < SERVER_NAMES.length; i++) {
      serverConfigurationMap.add(getServiceConfiguration(SERVER_NAMES[i],
                                                         hostName,
                                                         SERVER_PORTS[i],
                                                         hostName,
                                                         SERVER_PORTS[i] + 100));
      NODES[i] = new Node("localhost", SERVER_PORTS[i], SERVER_PORTS[i] + 100);
    }
  }

  private ServerConfiguration getServiceConfiguration(String serverName,
                                                      String tsaBindAddress,
                                                      int tsaPort,
                                                      String groupBindAddress,
                                                      int groupPort) {
    ServerConfiguration serverConfiguration = mock(ServerConfiguration.class);

    InetSocketAddress tsaPortMock = InetSocketAddress.createUnresolved(tsaBindAddress, tsaPort);
    InetSocketAddress groupPortMock = InetSocketAddress.createUnresolved(groupBindAddress, groupPort);

    when(serverConfiguration.getTsaPort()).thenReturn(tsaPortMock);
    when(serverConfiguration.getGroupPort()).thenReturn(groupPortMock);
    when(serverConfiguration.getName()).thenReturn(serverName);
    when(serverConfiguration.getHost()).thenReturn("localhost");

    return serverConfiguration;
  }
}