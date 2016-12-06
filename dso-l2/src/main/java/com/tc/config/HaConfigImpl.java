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

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import com.tc.net.groups.ServerGroup;
import com.tc.object.config.schema.L2Config;
import com.tc.util.Assert;

import java.util.HashSet;
import java.util.Set;

public class HaConfigImpl implements HaConfig {

  private final L2ConfigurationSetupManager configSetupManager;

  private final NodesStoreImpl              nodeStore;
  private final Node                        thisNode;
  private final ServerGroup                 thisGroup;

  public HaConfigImpl(L2ConfigurationSetupManager configSetupManager) {
    this.configSetupManager = configSetupManager;
    ActiveServerGroupConfig groupsConfig = this.configSetupManager.getActiveServerGroupForThisL2();
    this.thisGroup = new ServerGroup(groupsConfig);

    Set<Node> nodes = makeAllNodes();
    this.thisNode = makeThisNode();
    this.nodeStore = new NodesStoreImpl(nodes, getNodeNamesForThisGroup(),configSetupManager);
  }

  private Set<String> getNodeNamesForThisGroup() {
    Set<String> tmpSet = new HashSet<>();
    for (Node n : thisGroup.getNodes()) {
      tmpSet.add(n.getServerNodeName());
    }
    return tmpSet;
  }

  private Set<Node> makeAllNodes() {
    Set<Node> allClusterNodes = new HashSet<>();
    ActiveServerGroupConfig asgc = this.configSetupManager.getActiveServerGroupForThisL2();
      Assert.assertNotNull(asgc);
      String[] l2Names = asgc.getMembers();
      for (String l2Name : l2Names) {
        try {
          L2Config l2 = this.configSetupManager.dsoL2ConfigFor(l2Name);
          Node node = makeNode(l2);
          allClusterNodes.add(node);
          addNodeToGroup(node, l2Name);
        } catch (ConfigurationSetupException e) {
          throw new RuntimeException("Error getting l2 config for: " + l2Name, e);
        }
      }
    return allClusterNodes;
  }

  // servers and groups were checked in configSetupManger
  private void addNodeToGroup(Node node, String serverName) {
    boolean added = false;
      if (this.thisGroup.hasMember(serverName)) {
        thisGroup.addNode(node, serverName);
        added = true;
      }

      if (!added) { throw new AssertionError("Node=[" + node + "] with serverName=[" + serverName
                                           + "] was not added to any group!"); }
  }

  @Override
  public Node getThisNode() {
    return this.thisNode;
  }

  private Node makeThisNode() {
    L2Config l2 = this.configSetupManager.dsoL2Config();
    return makeNode(l2);
  }

  public static Node makeNode(L2Config l2) {
    String host = l2.tsaGroupPort().getBind();
    if (TCSocketAddress.WILDCARD_IP.equals(host)) {
      host = l2.host();
    }
    return new Node(host, l2.tsaPort().getValue(), l2.tsaGroupPort().getValue());
  }



  @Override
  public NodesStore getNodesStore() {
    return nodeStore;
  }


}
