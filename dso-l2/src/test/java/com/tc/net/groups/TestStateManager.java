/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.groups;


import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.net.NodeID;
import com.tc.util.State;

import java.util.concurrent.CopyOnWriteArrayList;

public class TestStateManager implements StateManager {
  private boolean                    isActive  = false;
  private final CopyOnWriteArrayList<StateChangeListener> listeners = new CopyOnWriteArrayList<>();
  private final NodeID               localNodeID;

  public TestStateManager(NodeID localNodeID) {
    this.localNodeID = localNodeID;
  }

  @Override
  public State getCurrentState() {
    return new State("NO_STATE");
  }

  @Override
  public void fireStateChangedEvent(StateChangedEvent sce) {
    for (StateChangeListener listener : listeners) {
      listener.l2StateChanged(sce);
    }
  }

  @Override
  public NodeID getActiveNodeID() {
    return localNodeID;
  }

  @Override
  public void handleClusterStateMessage(L2StateMessage clusterMsg) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isActiveCoordinator() {
    return isActive;
  }

  @Override
  public void moveNodeToPassiveStandby(NodeID nodeID) {
    isActive = false;
  }

  @Override
  public void moveToPassiveStandbyState() {
    isActive = false;
  }

  @Override
  public void publishActiveState(NodeID nodeID) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerForStateChangeEvents(StateChangeListener listener) {
    listeners.add(listener);
  }

  @Override
  public void startElection() {
    fireStateChangedEvent(new StateChangedEvent(StateManager.PASSIVE_STANDBY, StateManager.ACTIVE_COORDINATOR));
  }

  @Override
  public void startElectionIfNecessary(NodeID disconnectedNode) {
    throw new UnsupportedOperationException();
  }

  // added for testing purpose
  public void setActive(boolean status) {
    isActive = status;
  }

  public boolean isPassiveUnitialized() {
    return false;
  }

}
