/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.net.GroupID;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import com.tc.object.config.schema.L2Config;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class NodesStoreImpl implements NodesStore, TopologyChangeListener {
  private final Set<Node>                                   nodes;
  private final CopyOnWriteArraySet<TopologyChangeListener> listeners              = new CopyOnWriteArraySet<>();
  private L2ConfigurationSetupManager                       configSetupManager;
  private volatile Map<String, GroupID>                     nodeNameToGidMap       = new HashMap<>();
  private volatile Set<String>                              nodeNamesForThisGroup  = new HashSet<>();
  private volatile Map<String, String>                      nodeNamesToServerNames = new HashMap<>();

  /**
   * used for tests
   */
  public NodesStoreImpl(Set<Node> nodes) {
    this.nodes = Collections.synchronizedSet(nodes);
  }

  public NodesStoreImpl(Set<Node> nodes, Set<String> nodeNamesForThisGroup,
                        HashMap<String, GroupID> serverNodeNameToGidMap, L2ConfigurationSetupManager configSetupManager) {
    this(nodes);
    this.nodeNamesForThisGroup.addAll(nodeNamesForThisGroup);
    this.nodeNameToGidMap = serverNodeNameToGidMap;
    this.configSetupManager = configSetupManager;
    initNodeNamesToServerNames();
  }

  @Override
  public void topologyChanged(ReloadConfigChangeContext context) {
    this.nodes.addAll(context.getNodesAdded());
    this.nodes.removeAll(context.getNodesRemoved());
    initNodeNamesToServerNames();

    for (TopologyChangeListener listener : listeners) {
      listener.topologyChanged(context);
    }
  }

  @Override
  public void registerForTopologyChange(TopologyChangeListener listener) {
    listeners.add(listener);
  }

  @Override
  public Node[] getAllNodes() {
    Assert.assertTrue(this.nodes.size() > 0);
    return this.nodes.toArray(new Node[this.nodes.size()]);
  }

  private void initNodeNamesToServerNames() {
    HashMap<String, String> tempNodeNamesToServerNames = new HashMap<>();
    String[] serverNames = configSetupManager.allCurrentlyKnownServers();
    for (String serverName : serverNames) {
      try {
        L2Config l2Config = configSetupManager.dsoL2ConfigFor(serverName);
        String host = l2Config.tsaGroupPort().getBind();
        if (TCSocketAddress.WILDCARD_IP.equals(host)) {
          host = l2Config.host();
        }
        tempNodeNamesToServerNames.put(host + ":" + l2Config.tsaPort().getValue(), serverName);
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException(e);
      }
    }
    this.nodeNamesToServerNames = tempNodeNamesToServerNames;
  }

  @Override
  public String getServerNameFromNodeName(String nodeName) {

    return nodeNamesToServerNames.get(nodeName);
  }

  /**
   * ServerNamesOfThisGroup methods ...
   */

  @Override
  public boolean hasServerInGroup(String serverName) {
    return nodeNamesForThisGroup.contains(serverName);
  }

  void updateServerNames(ReloadConfigChangeContext context) {
    Set<String> tmp =  new HashSet<>(nodeNamesForThisGroup);

    for (Node n : context.getNodesAdded()) {
      tmp.add(n.getServerNodeName());
    }

    for (Node n : context.getNodesRemoved()) {
      tmp.remove(n.getServerNodeName());
    }

    this.nodeNamesForThisGroup = tmp;
  }

  /**
   * ServerNameGroupIDInfo methods ....
   */

  @Override
  public boolean hasServerInCluster(String name) {
    return nodeNameToGidMap.containsKey(name);
  }

  @Override
  public GroupID getGroupIDFromNodeName(String name) {
    return nodeNameToGidMap.get(name);
  }

  @Override
  public String getGroupNameFromNodeName(String nodeName) {
    if (configSetupManager == null) { return null; }
    ActiveServerGroupConfig asgc = configSetupManager.activeServerGroupsConfig()
        .getActiveServerGroupForL2(nodeNamesToServerNames.get(nodeName));
    if (asgc == null) { return null; }
    return asgc.getGroupName();
  }

  void updateServerNames(ReloadConfigChangeContext context, GroupID gid) {
    Map<String, GroupID> tempMap = new HashMap<>(nodeNameToGidMap);
    for (Node n : context.getNodesAdded()) {
      tempMap.put(n.getServerNodeName(), gid);
    }

    for (Node n : context.getNodesRemoved()) {
      tempMap.remove(n.getServerNodeName());
    }
    this.nodeNameToGidMap = tempMap;
  }
}
