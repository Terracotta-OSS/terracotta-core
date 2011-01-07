/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.NodeID;
import com.tc.util.State;

import java.util.Set;

public class L2PassiveSyncStateManager {

  private final L2IndexStateManager  indexStateManager;
  private final L2ObjectStateManager objectStateManager;

  public L2PassiveSyncStateManager(L2IndexStateManager indexStateManager, L2ObjectStateManager objectStateManager) {
    this.indexStateManager = indexStateManager;
    this.objectStateManager = objectStateManager;
  }

  /**
   * @return the number of L2s present in the cluster for which the index state is tracked. Note that the index state is
   *         not tracked for the local node.
   */
  public int getL2Count() {
    return this.objectStateManager.getL2Count();
  }

  public void removeL2(NodeID nodeID) {
    this.objectStateManager.removeL2(nodeID);
    this.indexStateManager.removeL2(nodeID);
  }

  public boolean addL2(NodeID nodeID, Set oids, State l2State) {
    return this.objectStateManager.addL2(nodeID, oids) && this.indexStateManager.addL2(nodeID, l2State);
  }
}
