/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.net.NodeID;

import java.util.Collection;
import java.util.Set;

public interface L2ObjectStateManager {

  /**
   * @return the number of L2s present in the cluster for which the object state is tracked. Note that the object state
   *         is not tracked for the local node.
   */
  public int getL2Count();

  public void removeL2(NodeID nodeID);

  public boolean addL2(NodeID nodeID, Set oids);

  public ManagedObjectSyncContext getSomeObjectsToSyncContext(NodeID nodeID, int count);

  public void close(ManagedObjectSyncContext mosc);

  public Collection getL2ObjectStates();

  public void registerForL2ObjectStateChangeEvents(L2ObjectStateListener listener);

  public void initiateSync(NodeID nodeID, Runnable syncRunnable);

  public void syncMore(NodeID nodeID);

  public void ackSync(NodeID nodeID);

}