/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.groups.NodeID;

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

}
