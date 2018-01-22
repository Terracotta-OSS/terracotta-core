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
package com.tc.l2.state;

import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.msg.L2StateMessage;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.text.PrettyPrintable;
import com.tc.util.State;


public interface StateManager extends PrettyPrintable {
  
  public final State       ACTIVE_COORDINATOR   = new State("ACTIVE-COORDINATOR");
  public final State       RECOVERING_STATE           = new State("RECOVERING");
  public final State       PASSIVE_UNINITIALIZED = new State("PASSIVE-UNINITIALIZED");
  public final State       PASSIVE_SYNCING = new State("PASSIVE-SYNCING");
  public final State       PASSIVE_STANDBY      = new State("PASSIVE-STANDBY");
  public final State       START_STATE          = new State("START-STATE");
  public final State       STOP_STATE           = new State("STOP-STATE");

  public void initializeAndStartElection();

  public ServerMode getCurrentMode();

  public void startElectionIfNecessary(NodeID disconnectedNode);

  public void registerForStateChangeEvents(StateChangeListener listener);

  public void fireStateChangedEvent(StateChangedEvent sce);

  public boolean isActiveCoordinator();

  public void moveToPassiveSyncing(NodeID connectedTo);
  
  public void moveToPassiveStandbyState();

  public void publishActiveState(NodeID nodeID) throws GroupException;

  public void handleClusterStateMessage(L2StateMessage clusterMsg);

  public NodeID getActiveNodeID();

  public void cleanupKnownServers();
  
  public static ServerMode convert(State state) {
    if (state == null) {
      return null;
    } else {
      return ServerMode.VALID_STATES.stream().filter(m->m.getState().equals(state)).findFirst().get();
    }
  }
}
