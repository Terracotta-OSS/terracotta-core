/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
