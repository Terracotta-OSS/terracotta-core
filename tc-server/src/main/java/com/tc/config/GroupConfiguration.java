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
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import org.terracotta.configuration.ServerConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupConfiguration {

  static final int SINGLE_SERVER_ELECTION_TIMEOUT = 0;
  static final int MULTI_SERVER_ELECTION_TIMEOUT = TCPropertiesImpl.getProperties().getInt(L2_ELECTION_TIMEOUT, 5);

  private final Map<String, Node> nodes;
  private final String serverName;

  GroupConfiguration(List<ServerConfiguration> servers, String serverName) {
    Map<String, Node> base = new HashMap<>();
    for (ServerConfiguration s : servers) {
      base.put(s.getName(), configToNode(s));
    }
    this.nodes = Collections.unmodifiableMap(base);
    this.serverName = serverName;
    if (MULTI_SERVER_ELECTION_TIMEOUT < 0) {
      throw new AssertionError("server election timeout cannot be less than zero");
    }
  }
  
  GroupConfiguration(InetSocketAddress target, GroupConfiguration parent) {
    Map<String, Node> base = new HashMap<>();
    base.put(target.getHostString() + ":" + target.getPort(), addressToNode(target));
    base.put(parent.serverName, parent.getCurrentNode());
    this.nodes = Collections.unmodifiableMap(base);
    this.serverName = parent.serverName;
  }

  public Set<Node> getNodes() {
    return new HashSet<>(nodes.values());
  }

  public Node getCurrentNode() {
    return nodes.get(serverName);
  }
  
  public GroupConfiguration directConnect(InetSocketAddress target) {
    return new GroupConfiguration(target, this);
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
  
  private static Node addressToNode(InetSocketAddress var) {
    return new Node(var.getHostName(), 0, var.getPort());
  }

  public int getElectionTimeInSecs() {
    //TODO fix the election time
    // If there is only one server, always going to win so no reason to wait
    return (nodes.size() == 1) ? SINGLE_SERVER_ELECTION_TIMEOUT : MULTI_SERVER_ELECTION_TIMEOUT;
  }

  public Set<String> getHostPorts() {
    return getNodes().stream().map(Node::getServerNodeName).collect(Collectors.toSet());
  }

  public String[] getMembers() {
    return this.nodes.keySet().stream().toArray(String[]::new);
  }
}
