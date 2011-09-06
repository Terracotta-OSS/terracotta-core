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

  public void objectSyncComplete(NodeID nodeID) {
    SyncValue value = syncMessagesProcessedMap.get(nodeID);
    if (value == null) {
      value = createSyncValue();
      syncMessagesProcessedMap.put(nodeID, value);
    }
    value.objectSyncCompleteMessageAcked();
  }

  public void indexSyncComplete(NodeID nodeID) {
    // overridden in EE
  }

  public void removeL2(NodeID nodeID) {
    syncMessagesProcessedMap.remove(nodeID);
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
    return value.isSyncMesssagesAcked();
  }

  static interface SyncValue {

    public void indexSyncCompleteMessageAcked();

    public void objectSyncCompleteMessageAcked();

    public boolean isSyncMesssagesAcked();
  }

  protected SyncValue createSyncValue() {

    return new SyncValue() {
      private volatile boolean objectSyncCompleteMessageProcessed = false;

      public void indexSyncCompleteMessageAcked() {
        // no-op
      }

      public void objectSyncCompleteMessageAcked() {
        this.objectSyncCompleteMessageProcessed = true;
      }

      public boolean isSyncMesssagesAcked() {
        return this.objectSyncCompleteMessageProcessed;
      }

    };
  }

}
