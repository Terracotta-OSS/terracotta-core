/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import com.tc.cluster.ClusterEventListener;
import com.tc.object.bytecode.ManagerUtil;

import java.util.HashSet;
import java.util.Set;

public class CacheParticipants implements ClusterEventListener {
  private String    nodeId;
  private final Set cacheParticipants = new HashSet();
  private boolean   registered        = false;

  public CacheParticipants() {
    // ManagerUtil.addClusterEventListener(this);
  }

  public synchronized void register() {
    ManagerUtil.addClusterEventListener(this);
    cacheParticipants.add(nodeId);
    registered = true;
  }

  public void nodeConnected(String nodeId) {
    if (!cacheParticipants.contains(nodeId)) cacheParticipants.add(nodeId);
  }

  public synchronized void nodeDisconnected(String nodeId) {
    if (cacheParticipants.contains(nodeId)) cacheParticipants.remove(nodeId);
  }

  public synchronized void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster) {
    this.nodeId = thisNodeId;
  }

  public void thisNodeDisconnected(String thisNodeId) {
    //
  }

  public synchronized Set getCacheParticipants() {
    return this.cacheParticipants;
  }

  public synchronized String getNodeId() {
    return this.nodeId;
  }

}
