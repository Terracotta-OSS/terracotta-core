/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import com.tc.net.groups.ServerGroup;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.util.ActiveCoordinatorHelper;
import com.tc.util.Assert;

import java.util.HashSet;
import java.util.Set;

public class HaConfigImpl implements HaConfig {

  private final L2TVSConfigurationSetupManager configSetupManager;
  private final ServerGroup[]                  groups;
  private final Node[]                         thisGroupNodes;
  private final Set                            allNodes;
  private final ServerGroup                    activeCoordinatorGroup;
  private final Node                           thisNode;
  private final ServerGroup                    thisGroup;

  public HaConfigImpl(L2TVSConfigurationSetupManager configSetupManager) {
    this.configSetupManager = configSetupManager;
    ActiveServerGroupsConfig groupsConfig = this.configSetupManager.activeServerGroupsConfig();
    int groupCount = groupsConfig.getActiveServerGroupCount();
    this.groups = new ServerGroup[groupCount];
    for (int i = 0; i < groupCount; i++) {
      this.groups[i] = new ServerGroup(groupsConfig.getActiveServerGroupArray()[i]);
    }
    int coodinatorIndex = ActiveCoordinatorHelper.getCoordinatorGroup(groupsConfig.getActiveServerGroupArray());
    this.activeCoordinatorGroup = coodinatorIndex != -1 ? this.groups[coodinatorIndex] : null;

    this.thisGroupNodes = makeThisGroupNodes();
    this.allNodes = makeAllNodes();
    this.thisNode = makeThisNode();
    this.thisGroup = getThisGroupFrom(this.groups, this.configSetupManager.getActiveServerGroupForThisL2());
  }

  private ServerGroup getThisGroupFrom(ServerGroup[] sg, ActiveServerGroupConfig activeServerGroupForThisL2) {
    for (int i = 0; i < sg.length; i++) {
      if (sg[i].getGroupId() == activeServerGroupForThisL2.getGroupId()) { return sg[i]; }
    }
    throw new RuntimeException("Unable to find this group information for " + this.thisNode + " "
                               + activeServerGroupForThisL2);
  }

  public boolean isActiveActive() {
    return this.configSetupManager.activeServerGroupsConfig().getActiveServerGroupCount() > 1;
  }

  public boolean isDiskedBasedActivePassive() {
    return !isActiveActive() && !isNetworkedActivePassive();
  }

  public boolean isNetworkedActivePassive() {
    return this.configSetupManager.haConfig().isNetworkedActivePassive();
  }

  public ServerGroup getActiveCoordinatorGroup() {
    return this.activeCoordinatorGroup;
  }

  public ServerGroup[] getAllActiveServerGroups() {
    return this.groups;
  }

  private Node[] makeThisGroupNodes() {
    ActiveServerGroupConfig asgc = this.configSetupManager.getActiveServerGroupForThisL2();
    Assert.assertNotNull(asgc);
    String[] l2Names = asgc.getMembers().getMemberArray();
    Node[] rv = new Node[l2Names.length];

    for (int i = 0; i < l2Names.length; i++) {
      NewL2DSOConfig l2;
      try {
        l2 = this.configSetupManager.dsoL2ConfigFor(l2Names[i]);
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException("Error getting l2 config for: " + l2Names[i], e);
      }
      rv[i] = makeNode(l2);
      addNodeToGroup(rv[i], l2Names[i]);
    }
    return rv;
  }

  public Node[] getThisGroupNodes() {
    return this.thisGroupNodes;
  }

  private Set makeAllNodes() {
    Set allClusterNodes = new HashSet();
    ActiveServerGroupConfig[] asgcs = this.configSetupManager.activeServerGroupsConfig().getActiveServerGroupArray();
    for (int j = 0; j < asgcs.length; ++j) {
      ActiveServerGroupConfig asgc = asgcs[j];
      Assert.assertNotNull(asgc);
      String[] l2Names = asgc.getMembers().getMemberArray();
      for (int i = 0; i < l2Names.length; i++) {
        try {
          NewL2DSOConfig l2 = this.configSetupManager.dsoL2ConfigFor(l2Names[i]);
          allClusterNodes.add(makeNode(l2));
        } catch (ConfigurationSetupException e) {
          throw new RuntimeException("Error getting l2 config for: " + l2Names[i], e);
        }
      }
    }
    return allClusterNodes;
  }

  public Node[] getAllNodes() {
    Assert.assertTrue(this.allNodes.size() > 0);
    return (Node[]) this.allNodes.toArray(new Node[this.allNodes.size()]);
  }

  // servers and groups were checked in configSetupManger
  private void addNodeToGroup(Node node, String serverName) {
    boolean added = false;
    for (int i = 0; i < this.groups.length; i++) {
      if (this.groups[i].hasMember(serverName)) {
        this.groups[i].addNode(node, serverName);
        added = true;
      }
    }
    if (!added) { throw new AssertionError("Node=[" + node + "] with serverName=[" + serverName
                                           + "] was not added to any group!"); }
  }

  public Node getThisNode() {
    return this.thisNode;
  }

  public ServerGroup getThisGroup() {
    return this.thisGroup;
  }

  private Node makeThisNode() {
    NewL2DSOConfig l2 = this.configSetupManager.dsoL2Config();
    return makeNode(l2);
  }

  private static Node makeNode(NewL2DSOConfig l2) {
    return new Node(l2.host().getString(), l2.listenPort().getInt(), l2.l2GroupPort().getInt(),
                    TCSocketAddress.WILDCARD_IP);
  }

  public boolean isActiveCoordinatorGroup() {
    return this.thisGroup == this.activeCoordinatorGroup;
  }
}
