/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.net.NodeID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateSyncManagerImpl implements StateSyncManager {

  protected final Map<NodeID, SyncValue> syncs              = new ConcurrentHashMap<NodeID, SyncValue>();

  protected volatile StateManager        stateManager;

  protected volatile boolean             objectSyncComplete = false;

  public StateSyncManagerImpl() {
    //
  }

  public void setStateManager(StateManager stateManager) {
    this.stateManager = stateManager;
  }

  public void objectSyncComplete() {
    final boolean move;

    synchronized (this) {
      objectSyncComplete = true;
      move = canMove();
    }

    if (move) {
      moveState();
    }
  }

  public boolean objectSyncComplete(NodeID nodeID) {
    SyncValue value = syncs.get(nodeID);
    if (value == null) {
      value = createSyncValue();
      syncs.put(nodeID, value);
    }

    value.objectSyncComplete();

    final boolean move = value.isSynced();

    if (move) {
      this.stateManager.moveNodeToPassiveStandby(nodeID);
    }

    return move;

  }

  public boolean isSyncComplete(NodeID nodeID) {
    SyncValue value = syncs.get(nodeID);
    if (value == null) { return false; }

    return value.isSynced();
  }

  public void indexSyncComplete() {
    // overridden in EE
  }

  protected boolean canMove() {
    return isIndexSyncComplete() && isObjectSyncComplete();
  }

  protected boolean isObjectSyncComplete() {
    return objectSyncComplete;
  }

  protected boolean isIndexSyncComplete() {
    // overridden in EE
    return true;
  }

  public boolean indexSyncComplete(NodeID nodeID) {
    // overridden in EE
    return true;
  }

  protected void moveState() {
    this.stateManager.moveToPassiveStandbyState();
  }

  static interface SyncValue {

    public void indexSyncComplete();

    public void objectSyncComplete();

    public boolean isSynced();
  }

  protected SyncValue createSyncValue() {
    return new SyncValue() {

      private volatile boolean objectSync = false;

      public void indexSyncComplete() {
        //
      }

      public void objectSyncComplete() {
        this.objectSync = true;
      }

      public boolean isSynced() {
        return this.objectSync;
      }

    };
  }

}
