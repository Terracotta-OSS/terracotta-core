/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.msg.GCResultMessage;
import com.tc.net.NodeID;

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

  public void handleGCResult(GCResultMessage message) {
    throw new UnsupportedOperationException();
  }

}
