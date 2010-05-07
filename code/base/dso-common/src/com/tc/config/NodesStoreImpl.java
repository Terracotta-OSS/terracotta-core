/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.GroupID;
import com.tc.net.groups.Node;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class NodesStoreImpl implements NodesStore, TopologyChangeListener {
  private final Set<Node>                                   nodes;
  public volatile HashSet<String>                           serverNamesForThisGroup = new HashSet<String>();
  private volatile HashMap<String, GroupID>                 serverNameToGidMap      = new HashMap<String, GroupID>();
  private final CopyOnWriteArraySet<TopologyChangeListener> listeners               = new CopyOnWriteArraySet<TopologyChangeListener>();

  /**
   * used for tests
   */
  public NodesStoreImpl(Set<Node> nodes) {
    this.nodes = Collections.synchronizedSet(nodes);
  }

  public NodesStoreImpl(Set<Node> nodes, Set<String> nodeNamesForThisGroup, HashMap<String, GroupID> serverNameToGidMap) {
    this(nodes);
    serverNamesForThisGroup.addAll(nodeNamesForThisGroup);
    this.serverNameToGidMap = serverNameToGidMap;
  }
  
  public void topologyChanged(ReloadConfigChangeContext context) {
    this.nodes.addAll(context.getNodesAdded());
    this.nodes.removeAll(context.getNodesRemoved());

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
