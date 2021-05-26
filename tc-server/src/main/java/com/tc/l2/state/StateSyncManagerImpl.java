/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.l2.state;

import com.tc.net.NodeID;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Object and Index Sync states for a L2. Active maintains the sync states for all the passives it tries to sync to.
 * Passives can check-point on sync completion messages if needed via hooks available here.
 */
public class StateSyncManagerImpl implements StateSyncManager {

  private final ConcurrentMap<NodeID, SyncValue> syncMessagesProcessedMap = new ConcurrentHashMap<>();

  @Override
  public void objectSyncComplete(NodeID nodeID) {
    SyncValue value = getOrCreateSyncValue(nodeID);
    value.objectSyncCompleteMessageAcked();
  }

  protected SyncValue getOrCreateSyncValue(NodeID nodeID) {
    SyncValue value = createSyncValue();
    SyncValue old = syncMessagesProcessedMap.putIfAbsent(nodeID, value);
    return old == null ? value : old;
  }

  @Override
  public void removeL2(NodeID nodeID) {
    syncMessagesProcessedMap.remove(nodeID);
  }

  @Override
  public void objectSyncComplete() {
    // no-op
  }

  @Override
  public boolean isSyncComplete(NodeID nodeID) {
    SyncValue value = syncMessagesProcessedMap.get(nodeID);
    if (value == null) { return false; }
    return value.isSyncMesssagesAcked();
  }

  static interface SyncValue {

    public void objectSyncCompleteMessageAcked();

    public boolean isSyncMesssagesAcked();
  }

  protected SyncValue createSyncValue() {

    return new SyncValue() {
      private volatile boolean objectSyncCompleteMessageProcessed = false;

      @Override
      public void objectSyncCompleteMessageAcked() {
        this.objectSyncCompleteMessageProcessed = true;
      }

      @Override
      public boolean isSyncMesssagesAcked() {
        return this.objectSyncCompleteMessageProcessed;
      }

    };
  }

}
