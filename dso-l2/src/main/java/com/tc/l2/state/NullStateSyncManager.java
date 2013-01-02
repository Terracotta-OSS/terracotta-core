/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.net.NodeID;

public class NullStateSyncManager implements StateSyncManager {

  @Override
  public void objectSyncComplete() {
    //
  }

  @Override
  public void indexSyncComplete() {
    //
  }

  @Override
  public void indexSyncComplete(NodeID nodeID) {
    //
  }

  @Override
  public boolean isSyncComplete(NodeID nodeID) {
    return true;
  }

  @Override
  public void objectSyncComplete(NodeID nodeID) {
    //
  }

  @Override
  public void removeL2(NodeID nodeID) {
    //
  }

}
