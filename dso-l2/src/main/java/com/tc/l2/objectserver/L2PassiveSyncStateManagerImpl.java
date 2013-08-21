/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.state.StateSyncManager;
import com.tc.net.NodeID;
import com.tc.util.State;

public class L2PassiveSyncStateManagerImpl implements L2PassiveSyncStateManager {

  protected final L2IndexStateManager  indexStateManager;
  protected final L2ObjectStateManager objectStateManager;
  protected final StateSyncManager     stateSyncManager;

  public L2PassiveSyncStateManagerImpl(L2IndexStateManager indexStateManager, L2ObjectStateManager objectStateManager,
                                       StateSyncManager stateSyncManager) {
    this.stateSyncManager = stateSyncManager;
    this.indexStateManager = indexStateManager;
    this.objectStateManager = objectStateManager;
  }

  /**
   * @return the number of L2s present in the cluster for which the index state is tracked. Note that the index state is
   *         not tracked for the local node.
   */
  @Override
  public int getL2Count() {
    return this.objectStateManager.getL2Count();
  }

  @Override
  public synchronized boolean addL2(NodeID nodeID, State l2State) {
    boolean objectAddL2 = this.objectStateManager.addL2(nodeID);
    boolean indexAddL2 = this.indexStateManager.addL2(nodeID, l2State);
    return objectAddL2 && indexAddL2;
  }

  @Override
  public synchronized void removeL2(NodeID nodeID) {
    this.stateSyncManager.removeL2(nodeID);
    this.objectStateManager.removeL2(nodeID);
    this.indexStateManager.removeL2(nodeID);
  }

  @Override
  public void objectSyncComplete() {
    this.stateSyncManager.objectSyncComplete();
  }

  @Override
  public void indexSyncComplete() {
    this.stateSyncManager.indexSyncComplete();
  }

  @Override
  public void objectSyncComplete(NodeID nodeID) {
    this.stateSyncManager.objectSyncComplete(nodeID);

  }

  @Override
  public void indexSyncComplete(NodeID nodeID) {
    this.stateSyncManager.indexSyncComplete(nodeID);
  }

  @Override
  public boolean isSyncComplete(NodeID nodeID) {
    return this.stateSyncManager.isSyncComplete(nodeID);
  }
}
