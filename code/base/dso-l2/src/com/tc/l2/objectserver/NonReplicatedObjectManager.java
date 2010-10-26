/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.msg.DGCResultMessage;
import com.tc.net.NodeID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;

public class NonReplicatedObjectManager implements ReplicatedObjectManager {

  public void query(NodeID nodeID) {
    // Nop
  }

  public boolean relayTransactions() {
    return false;
  }

  public void sync() {
    // Nop
  }

  public void clear(NodeID nodeID) {
    // Nop
  }

  public void handleGCResult(DGCResultMessage message) {
    throw new UnsupportedOperationException();
  }

  public void handleGCStartEvent(GarbageCollectionInfo gcInfo) {
    throw new UnsupportedOperationException();
  }

  public void handleGCCancelEvent(GarbageCollectionInfo gcInfo) {
    throw new UnsupportedOperationException();
  }

}
