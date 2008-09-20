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
import com.tc.util.Assert;

public class HaConfigImpl implements HaConfig {

  private final L2TVSConfigurationSetupManager configSetupManager;
  private final ServerGroup[]                  groups;
  private Node[]                               nodes;

  public HaConfigImpl(L2TVSConfigurationSetupManager configSetupManager) {
    this.configSetupManager = configSetupManager;
    ActiveServerGroupsConfig groupsConfig = this.configSetupManager.activeServerGroupsConfig();
    int groupCount = groupsConfig.getActiveServerGroupCount();
    this.groups = new ServerGroup[groupCount];
    for (int i = 0; i < groupCount; i++) {
      this.groups[i] = new ServerGroup(groupsConfig.getActiveServerGroupArray()[i]);
    }
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
    return groups != null ? groups[0] : null;
  }

  public ServerGroup[] getAllActiveServerGroups() {
    return this.groups;
  }

  public Node[] makeAllNodes() {
    ActiveServerGroupConfig asgc = configSetupManager.getActiveServerGroupForThisL2();
    Assert.assertNotNull(asgc);
    String[] l2Names = asgc.getMembers().getMemberArray();
    Node[] rv = new Node[l2Names.length];

    for (int i = 0; i < l2Names.length; i++) {
      NewL2DSOConfig l2;
      try {
        l2 = configSetupManager.dsoL2ConfigFor(l2Names[i]);
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException("Error getting l2 config for: " + l2Names[i], e);
      }
      rv[i] = makeNode(l2);
      addNodeToGroup(rv[i], l2Names[i]);
    }
    this.nodes = rv;
    return rv;
  }

  public Node[] getAllNodes() {
    return this.nodes;
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

  public Node makeThisNode() {
    NewL2DSOConfig l2 = configSetupManager.dsoL2Config();
    return makeNode(l2);
  }

  private static Node makeNode(NewL2DSOConfig l2) {
    return new Node(l2.host().getString(), l2.listenPort().getInt(), l2.l2GroupPort().getInt(), TCSocketAddress.WILDCARD_IP);
  }

}
