/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.NodeID;
import com.tc.util.State;

public class NullL2IndexStateManager implements L2IndexStateManager {

  @Override
  public boolean addL2(NodeID nodeID, State currentState) {
    return true;
  }

  @Override
  public void registerForL2IndexStateChangeEvents(L2IndexStateListener listener) {
    //
  }

  @Override
  public void removeL2(NodeID nodeID) {
    //
  }

  @Override
  public void initiateIndexSync(NodeID nodeID) {
    //
  }

  @Override
  public void receivedAck(NodeID nodeID, int amount) {
    //
  }

}
