/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.context.IndexSyncContext;
import com.tc.l2.context.NullIndexSyncContext;
import com.tc.net.NodeID;
import com.tc.util.State;

public class NullL2IndexStateManager implements L2IndexStateManager {

  public boolean addL2(NodeID nodeID, State currentState) {
    return false;
  }

  public void close(IndexSyncContext mosc) {
    //
  }

  public int getL2Count() {
    return 0;
  }

  public void registerForL2IndexStateChangeEvents(L2IndexStateListener listener) {
    //
  }

  public void removeL2(NodeID nodeID) {
    //
  }

  public IndexSyncContext getIndexToSyncContext(NodeID nodeID) {
    return new NullIndexSyncContext();
  }

}
