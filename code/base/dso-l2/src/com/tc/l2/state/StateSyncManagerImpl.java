/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.net.NodeID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Object and Index Sync states for a L2. Active maintains the sync states for all the passives it tries to sync to.
 * Passives can check-point on sync completion messages if needed via hooks available here.
 */
public class StateSyncManagerImpl implements StateSyncManager {

  protected final Map<NodeID, SyncValue> syncMessagesProcessedMap = new ConcurrentHashMap<NodeID, SyncValue>();
  protected volatile StateManager        stateManager;
  protected volatile boolean             objectSyncComplete       = false;

  public void setStateManager(StateManager stateManager) {
    this.stateManager = stateManager;
  }

  public void objectSyncComplete(NodeID nodeID) {
    SyncValue value = syncMessagesProcessedMap.get(nodeID);
    if (value == null) {
      value = createSyncValue();
      syncMessagesProcessedMap.put(nodeID, value);
    }
    value.setObjectSyncCompleteMessageProcessed();
  }

  public void indexSyncComplete(NodeID nodeID) {
    // overridden in EE
  }

  public void objectSyncComplete() {
    // no-op
  }

  public void indexSyncComplete() {
    // overridden in EE
  }

  public boolean isSyncComplete(NodeID nodeID) {
    SyncValue value = syncMessagesProcessedMap.get(nodeID);
    if (value == null) { return false; }
    return value.isSyncMesssagesProcessed();
  }

  static interface SyncValue {

    public void setIndexSyncCompleteMessageProcessed();

    public void setObjectSyncCompleteMessageProcessed();

    public boolean isSyncMesssagesProcessed();
  }

  protected SyncValue createSyncValue() {

    return new SyncValue() {
      private volatile boolean objectSyncCompleteMessageProcessed = false;

      public void setIndexSyncCompleteMessageProcessed() {
        // no-op
      }

      public void setObjectSyncCompleteMessageProcessed() {
        this.objectSyncCompleteMessageProcessed = true;
      }

      public boolean isSyncMesssagesProcessed() {
        return this.objectSyncCompleteMessageProcessed;
      }

    };
  }

}
