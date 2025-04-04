/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.l2.state;

import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.msg.L2StateMessage;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupException;
import com.tc.util.State;
import java.util.Set;
import com.tc.text.PrettyPrintable;


public interface StateManager extends PrettyPrintable {
  
  public final State       ACTIVE_COORDINATOR   = new State("ACTIVE-COORDINATOR");
  public final State       RECOVERING_STATE           = new State("RECOVERING");
  public final State       PASSIVE_UNINITIALIZED = new State("PASSIVE-UNINITIALIZED");
  public final State       PASSIVE_SYNCING = new State("PASSIVE-SYNCING");
  public final State       PASSIVE_STANDBY      = new State("PASSIVE-STANDBY");
  public final State       PASSIVE_RELAY      = new State("PASSIVE-RELAY");
  public final State       PASSIVE_DUPLICATE      = new State("PASSIVE-DUPLICATE");
  public final State       START_STATE          = new State("START-STATE");
  public final State       STOP_STATE           = new State("STOP-STATE");
  public final State       DIAGNOSTIC_STATE           = new State("DIAGNOSTIC");
  public final State       BOOTSTRAP_STATE           = new State("BOOTSTRAP");

  public void initializeAndStartElection();

  public ServerMode getCurrentMode();

  public void startElectionIfNecessary(NodeID disconnectedNode);

  public void registerForStateChangeEvents(StateChangeListener listener);

  public void fireStateChangedEvent(StateChangedEvent sce);

  public boolean isActiveCoordinator();

  public void moveToPassiveSyncing(NodeID connectedTo);
  
  public void moveToPassiveStandbyState();
  
  public void moveToDiagnosticMode();
  
  public void moveToRelayMode();
  
  public void moveToPassiveUnitialized();
  
  public boolean moveToStopStateIf(Set<ServerMode> validStates);

  public void publishActiveState(NodeID nodeID) throws GroupException;

  public void handleClusterStateMessage(L2StateMessage clusterMsg);

  public NodeID getActiveNodeID();
  
  public Set<ServerID> getPassiveStandbys();

  public void shutdown();
  
  public static ServerMode convert(State state) {
    if (state == null) {
      return ServerMode.START;
    } else {
      return ServerMode.VALID_STATES.stream().filter(m->m.getState().equals(state)).findFirst().get();
    }
  }
}
