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

  @Override
  public int getL2Count() {
    return 0;
  }

  @Override
  public void removeL2(NodeID nodeID) {
    //
  }

  @Override
  public boolean addL2(NodeID nodeID, Set oids) {
    return true;
  }

  @Override
  public ManagedObjectSyncContext getSomeObjectsToSyncContext(NodeID nodeID, int count) {
    return null;
  }

  @Override
  public void close(ManagedObjectSyncContext mosc) {
    //
  }

  @Override
  public Collection getL2ObjectStates() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public void registerForL2ObjectStateChangeEvents(L2ObjectStateListener listener) {
    //
  }

  @Override
  public void initiateSync(NodeID nodeID, Runnable dehydrateSyncObectsAndSendRunnable) {
    //
  }

  @Override
  public void syncMore(NodeID nodeID) {
    //
  }

  @Override
  public void ackSync(NodeID nodeID) {
    //
  }

}
