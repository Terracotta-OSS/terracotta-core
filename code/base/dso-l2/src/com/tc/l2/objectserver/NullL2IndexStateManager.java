/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.NodeID;
import com.tc.util.State;

public class NullL2IndexStateManager implements L2IndexStateManager {

  public boolean addL2(NodeID nodeID, State currentState) {
    return true;
  }

  public void registerForL2IndexStateChangeEvents(L2IndexStateListener listener) {
    //
  }

  public void removeL2(NodeID nodeID) {
    //
  }

  public void initiateIndexSync(NodeID nodeID) {
    //
  }

  public void receivedAck(NodeID nodeID, int amount) {
    //
  }

}
