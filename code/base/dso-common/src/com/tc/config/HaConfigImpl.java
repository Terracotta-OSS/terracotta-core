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
import com.tc.util.ActiveCoordintorHelper;
import com.tc.util.Assert;

import java.util.HashSet;
import java.util.Set;

public class HaConfigImpl implements HaConfig {

  private final L2TVSConfigurationSetupManager configSetupManager;
  private final ServerGroup[]                  groups;
  private final Node[]                         thisGroupNodes;
  private final Set                            allNodes = new HashSet();
  private final ServerGroup                    activeCoordinatorGroup;

  public HaConfigImpl(L2TVSConfigurationSetupManager configSetupManager) {
    this.configSetupManager = configSetupManager;
    ActiveServerGroupsConfig groupsConfig = this.configSetupManager.activeServerGroupsConfig();
    int groupCount = groupsConfig.getActiveServerGroupCount();
    this.groups = new ServerGroup[groupCount];
    for (int i = 0; i < groupCount; i++) {
      this.groups[i] = new ServerGroup(groupsConfig.getActiveServerGroupArray()[i]);
    }
    int coodinatorIndex = ActiveCoordintorHelper.getCoordinatorGroup(groupsConfig.getActiveServerGroupArray());
    activeCoordinatorGroup = coodinatorIndex != -1 ? groups[coodinatorIndex] : null;

    this.thisGroupNodes = makeThisGroupNodes();
    makeAllNodes();
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
    return activeCoordinatorGroup;
  }

  public ServerGroup[] getAllActiveServerGroups() {
    return this.groups;
  }

  private Node[] makeThisGroupNodes() {
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
    return rv;
  }

  public Node[] getThisGroupNodes() {
    return this.thisGroupNodes;
  }

  private void makeAllNodes() {
    ActiveServerGroupConfig[] asgcs = configSetupManager.activeServerGroupsConfig().getActiveServerGroupArray();
    for (int j = 0; j < asgcs.length; ++j) {
      ActiveServerGroupConfig asgc = asgcs[j];
      Assert.assertNotNull(asgc);
      String[] l2Names = asgc.getMembers().getMemberArray();
      for (int i = 0; i < l2Names.length; i++) {
        try {
          NewL2DSOConfig l2 = configSetupManager.dsoL2ConfigFor(l2Names[i]);
          allNodes.add(makeNode(l2));
        } catch (ConfigurationSetupException e) {
          throw new RuntimeException("Error getting l2 config for: " + l2Names[i], e);
        }
      }
    }
  }

  public Node[] getAllNodes() {
    Assert.assertTrue(allNodes.size() > 0);
    return (Node[]) allNodes.toArray(new Node[allNodes.size()]);
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
    return new Node(l2.host().getString(), l2.listenPort().getInt(), l2.l2GroupPort().getInt(),
                    TCSocketAddress.WILDCARD_IP);
  }
}
