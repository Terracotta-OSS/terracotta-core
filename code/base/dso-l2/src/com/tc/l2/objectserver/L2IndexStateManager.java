/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.context.IndexSyncContext;
import com.tc.net.NodeID;

public interface L2IndexStateManager {

  /**
   * @return the number of L2s present in the cluster for which the index state is tracked. Note that the index state is
   *         not tracked for the local node.
   */
  public int getL2Count();

  public void removeL2(NodeID nodeID);

  public boolean addL2(NodeID nodeID, boolean syncIndex);

  public IndexSyncContext getIndexToSyncContext(final NodeID nodeID);

  public void close(IndexSyncContext isc);

  public void registerForL2IndexStateChangeEvents(L2IndexStateListener listener);

}