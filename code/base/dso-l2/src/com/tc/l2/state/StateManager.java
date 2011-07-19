/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.msg.L2StateMessage;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.util.State;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface StateManager {

  public static final State       ACTIVE_COORDINATOR   = new State("ACTIVE-COORDINATOR");
  public static final State       PASSIVE_UNINTIALIZED = new State("PASSIVE-UNINITIALIZED");
  public static final State       PASSIVE_STANDBY      = new State("PASSIVE-STANDBY");
  public static final State       START_STATE          = new State("START-STATE");
  public static final State       STOP_STATE           = new State("STOP-STATE");
  public static final List<State> validStates          = Collections.unmodifiableList(Arrays
                                                           .asList(START_STATE, PASSIVE_UNINTIALIZED, PASSIVE_STANDBY,
                                                                   ACTIVE_COORDINATOR, STOP_STATE));

  public void startElection();

  public State getCurrentState();

  public void startElectionIfNecessary(NodeID disconnectedNode);

  public void registerForStateChangeEvents(StateChangeListener listener);

  public void fireStateChangedEvent(StateChangedEvent sce);

  public boolean isActiveCoordinator();

  public void moveNodeToPassiveStandby(NodeID nodeID);

  public void moveToPassiveStandbyState();

  public void publishActiveState(NodeID nodeID) throws GroupException;

  public void handleClusterStateMessage(L2StateMessage clusterMsg);

  public NodeID getActiveNodeID();

}
