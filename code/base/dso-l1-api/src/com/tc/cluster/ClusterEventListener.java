/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import java.util.EventListener;

/**
 * Listener for cluster events that occur when the cluster changes.
 */
public interface ClusterEventListener extends EventListener {
  
  /**
   * New node is connected to the cluster 
   * @param nodeId Id of new node
   */
  void nodeConnected(String nodeId);

  /**
   * Node has left cluster
   * @param nodeId Id of leaving node
   */
  void nodeDisconnected(String nodeId);

  /**
   * Current node has disconnected from the cluster
   * @param thisNodeId Current node Id
   */
  void thisNodeDisconnected(String thisNodeId);

  /**
   * Current node has connected to the cluster
   * @param thisNodeId Current node id
   * @param nodesCurrentlyInCluster Nodes already in cluster on connect
   */
  void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster);
}
