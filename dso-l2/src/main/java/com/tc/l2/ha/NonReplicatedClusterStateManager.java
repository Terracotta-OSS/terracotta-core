/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.net.NodeID;
import com.tc.util.State;

public class NonReplicatedClusterStateManager implements ReplicatedClusterStateManager {

  @Override
  public void goActiveAndSyncState() {
    // NoP
  }

  @Override
  public void publishClusterState(NodeID nodeID) {
    // Nop
  }

  @Override
  public void publishNextAvailableObjectID(long l) {
    // Nop
  }

  @Override
  public void publishNextAvailableGlobalTransactionID(long l) {
    // Nop
  }

  @Override
  public void fireNodeLeftEvent(NodeID nodeID) {
    // Nop
  }

  @Override
  public void setCurrentState(State currentState) {
    // Nop
  }

  @Override
  public void publishNextAvailableDGCID(long nextGcIteration) {
    // Nop
  }

}
