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

import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import static com.tc.properties.TCPropertiesConsts.L2_ELECTION_TIMEOUT;
import com.tc.properties.TCPropertiesImpl;
import org.terracotta.configuration.ServerConfiguration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupConfiguration {

  static final int SINGLE_SERVER_ELECTION_TIMEOUT = 0;
  static final int MULTI_SERVER_ELECTION_TIMEOUT = TCPropertiesImpl.getProperties().getInt(L2_ELECTION_TIMEOUT, 5);

  private final List<ServerConfiguration> configs;
  private final String serverName;

  GroupConfiguration(List<ServerConfiguration> servers, String serverName) {
    this.configs = servers;
    this.serverName = serverName;
    if (MULTI_SERVER_ELECTION_TIMEOUT < 0) {
      throw new AssertionError("server election timeout cannot be less than zero");
    }
  }

  public Set<Node> getNodes() {
    return this.configs.stream().map(GroupConfiguration::configToNode).collect(Collectors.toSet());
  }

  public Node getCurrentNode() {
    return this.configs.stream().filter(c->c.getName().equals(serverName)).findFirst().map(GroupConfiguration::configToNode).orElseThrow(()->new RuntimeException("invalid configuration"));
  }
  
  private static Node configToNode(ServerConfiguration sc) {
    String bindAddress = sc.getTsaPort().getHostName();
      if (TCSocketAddress.isWildcardAddress(bindAddress)) {
        bindAddress = sc.getHost();
      }
      return new Node(bindAddress,
                           sc.getTsaPort().getPort(),
                           sc.getGroupPort().getPort());
  }

  public int getElectionTimeInSecs() {
    //TODO fix the election time
    // If there is only one server, always going to win so no reason to wait
    return (configs.size() == 1) ? SINGLE_SERVER_ELECTION_TIMEOUT : MULTI_SERVER_ELECTION_TIMEOUT;
  }

  public Set<String> getHostPorts() {
    return getNodes().stream().map(Node::getServerNodeName).collect(Collectors.toSet());
  }

  public String[] getMembers() {
    return this.configs.stream().map(ServerConfiguration::getName).toArray(String[]::new);
  }
}
