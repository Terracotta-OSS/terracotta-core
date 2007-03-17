/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.async.api.Sink;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.net.groups.NodeID;
import com.tc.objectserver.api.ObjectManager;

import java.util.List;
import java.util.Set;

public interface L2ObjectStateManager {

  public void removeL2(NodeID nodeID);

  public int addL2WithObjectIDs(NodeID nodeID, Set oids, ObjectManager objectManager);

  public ManagedObjectSyncContext getSomeObjectsToSyncContext(NodeID nodeID, int count, Sink sink);

  public void close(ManagedObjectSyncContext mosc);
  
  public List getL2ObjectStates();
}