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

  public void fireStateChangedEvent(StateChangedEvent sce) {
    for (StateChangeListener element : stateListeners) {
      StateChangeListener listener = element;
      listener.l2StateChanged(sce);
    }
  }

  public State getCurrentState() {
    return new State("NO_STATE");
  }

  public boolean isActiveCoordinator() {
    return true;
  }

  public boolean isPassiveUnitialized() {
    return false;
  }

  public void moveNodeToPassiveStandby(NodeID nodeID) {
    throw new UnsupportedOperationException();
  }

  public void registerForStateChangeEvents(StateChangeListener listener) {
    stateListeners.add(listener);
  }

  public void startElection() {
    // No need to start election, if we are here, we are active, notify it.
    fireStateChangedEvent(new StateChangedEvent(StateManager.PASSIVE_STANDBY, StateManager.ACTIVE_COORDINATOR));
  }

  public void publishActiveState(NodeID nodeID) {
    // Nop
  }

  public void startElectionIfNecessary(NodeID disconnectedNode) {
    // Nop
  }

  public void moveToPassiveStandbyState() {
    throw new UnsupportedOperationException();
  }

  public void handleClusterStateMessage(L2StateMessage clusterMsg) {
    throw new UnsupportedOperationException();
  }

  public NodeID getActiveNodeID() {
    return localNodeID;
  }

}
