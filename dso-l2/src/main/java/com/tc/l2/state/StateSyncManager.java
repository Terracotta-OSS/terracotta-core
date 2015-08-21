/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.net.NodeID;

public interface StateSyncManager {

  void objectSyncComplete();

  void objectSyncComplete(NodeID nodeID);

  boolean isSyncComplete(NodeID nodeID);

  public void removeL2(NodeID nodeID);

}
