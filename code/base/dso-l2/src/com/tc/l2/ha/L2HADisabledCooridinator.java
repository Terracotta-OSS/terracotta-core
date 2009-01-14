/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.objectserver.NonReplicatedObjectManager;
import com.tc.l2.objectserver.NonReplicatedTransactionManager;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.l2.objectserver.ReplicatedTransactionManager;
import com.tc.l2.state.DummyStateManager;
import com.tc.l2.state.StateManager;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.SingleNodeGroupManager;

public class L2HADisabledCooridinator implements L2Coordinator {

  private final GroupManager                  groupManager;
  private final StateManager                  stateMgr;
  private final ReplicatedClusterStateManager clusterStateMgr  = new NonReplicatedClusterStateManager();
  private final ReplicatedObjectManager       replicatedObjMgr = new NonReplicatedObjectManager();
  private final ReplicatedTransactionManager  replicatedTxnMgr = new NonReplicatedTransactionManager();

  public L2HADisabledCooridinator(GroupManager groupCommManager) {
    this.groupManager = groupCommManager;
    this.stateMgr = new DummyStateManager(this.groupManager.getLocalNodeID());
  }

  public L2HADisabledCooridinator() {
    this(new SingleNodeGroupManager());
  }

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
    // Give the state manager an opportunity to notify that we are active to interested parties.
    stateMgr.startElection();
  }

  public ReplicatedTransactionManager getReplicatedTransactionManager() {
    return replicatedTxnMgr;
  }

}
