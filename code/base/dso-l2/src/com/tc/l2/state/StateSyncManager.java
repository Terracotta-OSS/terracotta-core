/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.net.NodeID;

public interface StateSyncManager {

  void objectSyncComplete();

  void indexSyncComplete();

  boolean objectSyncComplete(NodeID nodeID);

  boolean indexSyncComplete(NodeID nodeID);

  boolean isSyncComplete(NodeID nodeID);

  void setStateManager(StateManager stateManager);

}
