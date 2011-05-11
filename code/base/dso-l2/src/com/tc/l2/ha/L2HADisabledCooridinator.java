/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.NonReplicatedObjectManager;
import com.tc.l2.objectserver.NonReplicatedTransactionManager;
import com.tc.l2.objectserver.NullL2ObjectStateManager;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.l2.objectserver.ReplicatedTransactionManager;
import com.tc.l2.state.DummyStateManager;
import com.tc.l2.state.NullStateSyncManager;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateSyncManager;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.SingleNodeGroupManager;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

public class L2HADisabledCooridinator implements L2Coordinator, PrettyPrintable {

  private final GroupManager                  groupManager;
  private final StateManager                  stateMgr;
  private final ReplicatedClusterStateManager clusterStateMgr    = new NonReplicatedClusterStateManager();
  private final ReplicatedObjectManager       replicatedObjMgr   = new NonReplicatedObjectManager();
  private final ReplicatedTransactionManager  replicatedTxnMgr   = new NonReplicatedTransactionManager();
  private final StateSyncManager              stateSyncManager   = new NullStateSyncManager();
  private final L2ObjectStateManager          objectStateManager = new NullL2ObjectStateManager();

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

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print(this.getClass().getSimpleName()).flush();
    StringBuilder strBuffer = new StringBuilder();
    strBuffer.append("L2HADisabledCooridinator [ ");
    strBuffer.append(this.stateMgr);
    strBuffer.append(" ]");
    out.indent().print(strBuffer.toString()).flush();
    return out;
  }

  public StateSyncManager getStateSyncManager() {
    return stateSyncManager;
  }

  public void l2StateChanged(StateChangedEvent sce) {
    //
  }

  public L2ObjectStateManager getL2ObjectStateManager() {
    return objectStateManager;
  }

}
