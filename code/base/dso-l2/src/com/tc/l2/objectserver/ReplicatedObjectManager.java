/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.msg.GCResultMessage;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;

public interface ReplicatedObjectManager {

  /**
   * This method is used to sync up all ObjectIDs from the remote ObjectManagers. It is synchronous and after when it
   * returns nobody is allowed to join the cluster with exisiting objects.
   */
  public void sync();

  public boolean relayTransactions();

  public void query(NodeID nodeID) throws GroupException;
  
  public void clear(NodeID nodeID);

  public void handleGCResult(GCResultMessage message);

}