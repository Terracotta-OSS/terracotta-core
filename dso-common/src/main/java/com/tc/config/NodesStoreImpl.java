/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.net.GroupID;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class NodesStoreImpl implements NodesStore, TopologyChangeListener {
  private final Set<Node>                                   nodes;
  private final CopyOnWriteArraySet<TopologyChangeListener> listeners               = new CopyOnWriteArraySet<TopologyChangeListener>();
  private L2ConfigurationSetupManager                    configSetupManager;
  private volatile HashMap<String, GroupID>                 serverNameToGidMap      = new HashMap<String, GroupID>();
  private volatile HashSet<String>                          serverNamesForThisGroup = new HashSet<String>();
  private volatile HashMap<String, String>                  nodeNamesToServerNames  = new HashMap<String, String>();

  /**
   * used for tests
   */
  public NodesStoreImpl(Set<Node> nodes) {
    this.nodes = Collections.synchronizedSet(nodes);
  }

  public NodesStoreImpl(Set<Node> nodes, Set<String> nodeNamesForThisGroup,
                        HashMap<String, GroupID> serverNameToGidMap, L2ConfigurationSetupManager configSetupManager) {
    this(nodes);
    serverNamesForThisGroup.addAll(nodeNamesForThisGroup);
    this.serverNameToGidMap = serverNameToGidMap;
    this.configSetupManager = configSetupManager;
    initNodeNamesToServerNames();
  }

  public void topologyChanged(ReloadConfigChangeContext context) {
    this.nodes.addAll(context.getNodesAdded());
    this.nodes.removeAll(context.getNodesRemoved());
    initNodeNamesToServerNames();

    for (TopologyChangeListener listener : listeners) {
      listener.topologyChanged(context);
    }
  }

  public void registerForTopologyChange(TopologyChangeListener listener) {
    listeners.add(listener);
  }

  public Node[] getAllNodes() {
    Assert.assertTrue(this.nodes.size() > 0);
    return this.nodes.toArray(new Node[this.nodes.size()]);
  }

  private void initNodeNamesToServerNames() {
    HashMap<String, String> tempNodeNamesToServerNames = new HashMap<String, String>();
    String[] serverNames = configSetupManager.allCurrentlyKnownServers();
    for (int i = 0; i < serverNames.length; i++) {
      try {
        L2DSOConfig l2Config = configSetupManager.dsoL2ConfigFor(serverNames[i]);
        String host = l2Config.l2GroupPort().getBind();
        if (TCSocketAddress.WILDCARD_IP.equals(host)) {
          host = l2Config.host();
        }
        tempNodeNamesToServerNames.put(host + ":" + l2Config.dsoPort().getIntValue(), serverNames[i]);
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException(e);
      }
    }
    this.nodeNamesToServerNames = tempNodeNamesToServerNames;
  }

  public String getNodeNameFromServerName(String serverName) {

    return nodeNamesToServerNames.get(serverName);
  }

  /**
   * ServerNamesOfThisGroup methods ...
   */

  public boolean hasServerInGroup(String serverName) {
    return serverNamesForThisGroup.contains(serverName);
  }

  void updateServerNames(ReloadConfigChangeContext context) {
    HashSet<String> tmp = (HashSet<String>) serverNamesForThisGroup.clone();

    for (Node n : context.getNodesAdded()) {
      tmp.add(n.getServerNodeName());
    }

    for (Node n : context.getNodesRemoved()) {
      tmp.remove(n.getServerNodeName());
    }

    this.serverNamesForThisGroup = tmp;
  }

  /**
   * ServerNameGroupIDInfo methods ....
   */

  public boolean hasServerInCluster(String name) {
    return serverNameToGidMap.containsKey(name);
  }

  public GroupID getGroupIDFromServerName(String name) {
    return serverNameToGidMap.get(name);
  }

  void updateServerNames(ReloadConfigChangeContext context, GroupID gid) {
    HashMap<String, GroupID> tempMap = (HashMap<String, GroupID>) serverNameToGidMap.clone();
    for (Node n : context.getNodesAdded()) {
      tempMap.put(n.getServerNodeName(), gid);
    }

    for (Node n : context.getNodesRemoved()) {
      tempMap.remove(n.getServerNodeName());
    }
    this.serverNameToGidMap = tempMap;
  }
}
