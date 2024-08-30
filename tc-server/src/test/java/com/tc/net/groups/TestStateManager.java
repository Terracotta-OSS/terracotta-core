/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.groups;

import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CopyOnWriteArrayList;

public class TestStateManager implements StateManager {

  private boolean isActive = false;
  private final CopyOnWriteArrayList<StateChangeListener> listeners = new CopyOnWriteArrayList<>();
  private final NodeID localNodeID;

  public TestStateManager(NodeID localNodeID) {
    this.localNodeID = localNodeID;
  }

  @Override
  public ServerMode getCurrentMode() {
    return ServerMode.STOP;
  }

  @Override
  public void fireStateChangedEvent(StateChangedEvent sce) {
    for (StateChangeListener listener : listeners) {
      listener.l2StateChanged(sce);
    }
  }

  @Override
  public void shutdown() {

  }

  @Override
  public NodeID getActiveNodeID() {
    return localNodeID;
  }

  @Override
  public Set<ServerID> getPassiveStandbys() {
    return Collections.emptySet();
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
  public void moveToPassiveSyncing(NodeID connectedTo) {
    isActive = false;
  }

  @Override
  public void moveToPassiveStandbyState() {
    isActive = false;
  }

  @Override
  public void moveToStopState() {
    isActive = false;
  }

  @Override
  public void moveToDiagnosticMode() {

  }
  
  @Override
  public void moveToRelayMode() {

  }  

  @Override
  public void moveToPassiveUnitialized() {

  }

  @Override
  public boolean moveToStopStateIf(Set<ServerMode> validStates) {
    isActive = false;
    return false;
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
  public void initializeAndStartElection() {
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

  @Override
  public Map<String, ?> getStateMap() {
    return Collections.emptyMap();
  }

}
