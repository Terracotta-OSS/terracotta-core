/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.objectserver.NonReplicatedObjectManager;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.l2.state.DummyStateManager;
import com.tc.l2.state.StateManager;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.SingleNodeGroupManager;

public class L2HADisabledCooridinator implements L2Coordinator {

  private GroupManager                  groupManager     = new SingleNodeGroupManager();
  private ReplicatedClusterStateManager clusterStateMgr  = new NonReplicatedClusterStateManager();
  private ReplicatedObjectManager       replicatedObjMgr = new NonReplicatedObjectManager();
  private StateManager                  stateMgr         = new DummyStateManager();

  public GroupManager getGroupManager() {
    return groupManager;
  }

  public ReplicatedClusterStateManager getReplicatedClusterStateManager() {
    return clusterStateMgr;
  }

  public ReplicatedObjectManager getReplicatedObjectManager() {
    return replicatedObjMgr;
  }

  public StateManager getStateManager() {
    return stateMgr;
  }

  public void start() {
    // Nop
  }

}
