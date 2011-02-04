/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.NodeID;
import com.tc.util.State;

import java.util.Set;

public interface L2PassiveSyncStateManager {

  /**
   * @return the number of L2s present in the cluster for which the index state is tracked. Note that the index state is
   *         not tracked for the local node.
   */
  public abstract int getL2Count();

  public abstract void removeL2(NodeID nodeID);

  public abstract boolean addL2(NodeID nodeID, Set oids, State l2State);

}