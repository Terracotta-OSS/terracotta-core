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

  public void indexSyncComplete(NodeID nodeID) {
    //
  }

  public boolean isSyncComplete(NodeID nodeID) {
    return true;
  }

  public void objectSyncComplete(NodeID nodeID) {
    //
  }

  public void removeL2(NodeID nodeID) {
    //
  }

}
