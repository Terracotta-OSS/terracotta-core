/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.l2.context.StateChangedEvent;
import com.tc.net.groups.NodeID;
import com.tc.util.State;

public interface StateManager {

  public static final State   ACTIVE_COORDINATOR   = new State("ACTIVE-COORDINATOR");
  public static final State   PASSIVE_UNINTIALIZED = new State("PASSIVE-UNINITIALIZED");
  public static final State   PASSIVE_STANDBY      = new State("PASSIVE-STANDBY");
  public static final State   START_STATE          = new State("START-STATE");
  public static final State[] validStates          = new State[] { START_STATE, PASSIVE_UNINTIALIZED, PASSIVE_STANDBY,
      ACTIVE_COORDINATOR                          };

  public void startElection();

  public void registerForStateChangeEvents(StateChangeListener listener);

  public void fireStateChangedEvent(StateChangedEvent sce);

  public boolean isActiveCoordinator();

  public void moveNodeToPassiveStandby(NodeID nodeID);

}
