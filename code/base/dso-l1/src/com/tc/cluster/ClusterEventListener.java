/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import java.util.EventListener;

public interface ClusterEventListener extends EventListener {
  
  void nodeConnected(String nodeId);

  void nodeDisconnected(String nodeId);

  void thisNodeDisconnected(String thisNodeId);

  void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster);
}
