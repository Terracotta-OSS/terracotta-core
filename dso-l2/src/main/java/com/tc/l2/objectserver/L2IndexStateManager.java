/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.NodeID;
import com.tc.util.State;

public interface L2IndexStateManager {

  public void removeL2(NodeID nodeID);

  public boolean addL2(NodeID nodeID, State l2State);

  public void registerForL2IndexStateChangeEvents(L2IndexStateListener listener);

  public void initiateIndexSync(NodeID nodeID);

  public void receivedAck(NodeID nodeID, int amount);

}