/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.api;

import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.l2.objectserver.ReplicatedTransactionManager;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateSyncManager;
import com.tc.net.groups.GroupManager;
import com.tc.text.PrettyPrintable;

public interface L2Coordinator extends StateChangeListener, PrettyPrintable {

  public void start();

  public ReplicatedClusterStateManager getReplicatedClusterStateManager();

  public ReplicatedObjectManager getReplicatedObjectManager();

  public ReplicatedTransactionManager getReplicatedTransactionManager();

  public StateManager getStateManager();

  public GroupManager getGroupManager();

  public StateSyncManager getStateSyncManager();

}
