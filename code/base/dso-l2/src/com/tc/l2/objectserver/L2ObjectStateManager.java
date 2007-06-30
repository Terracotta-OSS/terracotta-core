/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.async.api.Sink;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.net.groups.NodeID;

import java.util.Collection;
import java.util.Set;

public interface L2ObjectStateManager {

  /**
   * @return the number of L2s present in the cluster for which the object state is tracked. Note that the object state
   *         is not tracked for the local node.
   */
  public int getL2Count();

  public void removeL2(NodeID nodeID);

  public void addL2(NodeID nodeID, Set oids);

  public ManagedObjectSyncContext getSomeObjectsToSyncContext(NodeID nodeID, int count, Sink sink);

  public void close(ManagedObjectSyncContext mosc);

  public Collection getL2ObjectStates();

  public void registerForL2ObjectStateChangeEvents(L2ObjectStateListener listener);

}