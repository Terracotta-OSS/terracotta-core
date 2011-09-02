/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.net.NodeID;
import com.tc.util.State;

public class NonReplicatedClusterStateManager implements ReplicatedClusterStateManager {

  public void goActiveAndSyncState() {
    // NoP
  }

  public void publishClusterState(NodeID nodeID) {
    // Nop
  }

  public void publishNextAvailableObjectID(long l) {
    // Nop
  }

  public void publishNextAvailableGlobalTransactionID(long l) {
    // Nop
  }

  public void fireNodeLeftEvent(NodeID nodeID) {
    // Nop
  }

  public void setCurrentState(State currentState) {
    // Nop
  }

  public void publishNextAvailableDGCID(long nextGcIteration) {
    // Nop
  }

}
