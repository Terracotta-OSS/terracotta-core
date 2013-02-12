/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.msg.L2StateMessage;
import com.tc.net.NodeID;
import com.tc.util.State;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DummyStateManager implements StateManager {

  private final List<StateChangeListener> stateListeners = new CopyOnWriteArrayList<StateChangeListener>();
  private final NodeID                    localNodeID;

  public DummyStateManager(NodeID localNodeID) {
    this.localNodeID = localNodeID;
  }

  @Override
  public void fireStateChangedEvent(StateChangedEvent sce) {
    for (StateChangeListener element : stateListeners) {
      StateChangeListener listener = element;
      listener.l2StateChanged(sce);
    }
  }

  @Override
  public State getCurrentState() {
    return new State("NO_STATE");
  }

  @Override
  public boolean isActiveCoordinator() {
    return true;
  }

  public boolean isPassiveUnitialized() {
    return false;
  }

  @Override
  public void moveNodeToPassiveStandby(NodeID nodeID) {
    throw new UnsupportedOperationException();
  }


  @Override
  public void registerForStateChangeEvents(StateChangeListener listener) {
    stateListeners.add(listener);
  }

  @Override
  public void startElection(boolean isNew) {
    // No need to start election, if we are here, we are active, notify it.
    fireStateChangedEvent(new StateChangedEvent(StateManager.PASSIVE_STANDBY, StateManager.ACTIVE_COORDINATOR));
  }

  @Override
  public void publishActiveState(NodeID nodeID) {
    // Nop
  }

  @Override
  public void startElectionIfNecessary(NodeID disconnectedNode) {
    // Nop
  }

  @Override
  public void moveToPassiveStandbyState() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void handleClusterStateMessage(L2StateMessage clusterMsg) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NodeID getActiveNodeID() {
    return localNodeID;
  }

}
