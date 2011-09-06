/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.net.NodeID;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class NullL2ObjectStateManager implements L2ObjectStateManager {

  public int getL2Count() {
    return 0;
  }

  public void removeL2(NodeID nodeID) {
    //
  }

  public boolean addL2(NodeID nodeID, Set oids) {
    return true;
  }

  public ManagedObjectSyncContext getSomeObjectsToSyncContext(NodeID nodeID, int count) {
    return null;
  }

  public void close(ManagedObjectSyncContext mosc) {
    //
  }

  public Collection getL2ObjectStates() {
    return Collections.EMPTY_LIST;
  }

  public void registerForL2ObjectStateChangeEvents(L2ObjectStateListener listener) {
    //
  }

  public void initiateSync(NodeID nodeID, Runnable dehydrateSyncObectsAndSendRunnable) {
    //
  }

  public void syncMore(NodeID nodeID) {
    //
  }

  public void ackSync(NodeID nodeID) {
    //
  }

}
