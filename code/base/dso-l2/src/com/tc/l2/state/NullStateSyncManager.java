/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.net.NodeID;

public class NullStateSyncManager implements StateSyncManager {

  public void objectSyncComplete() {
    //
  }

  public void indexSyncComplete() {
    //
  }

  public void setStateManager(StateManager stateManager) {
    //
  }

  public boolean indexSyncComplete(NodeID nodeID) {
    return true;
  }

  public boolean isSyncComplete(NodeID nodeID) {
    return true;
  }

  public boolean objectSyncComplete(NodeID nodeID) {
    return true;
  }

}
