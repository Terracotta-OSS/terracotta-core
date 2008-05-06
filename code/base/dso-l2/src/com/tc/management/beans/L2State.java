/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.util.State;

public class L2State implements StateChangeListener {
  private static final boolean DEBUG       = false;
  private State                serverState = StateManager.START_STATE;
  private StateChangeListener  changeListener;

  public synchronized void setState(State state) {
    if (!validateState(state)) { throw new AssertionError("Unrecognized server state: [" + state.getName() + "]"); }

    // TODO: need to deal with checking for state validity at some point
    // if (serverState.equals(state)) { throw new AssertionError("Re-setting L2 state to the same state:
    // existing=["+serverState.getName()+"] passedIn=["+state.getName()+"]"); }

    if (changeListener != null) {
      debugPrintln("*******  L2State is notifying listener of state change:  oldState=[" + serverState.getName()
                   + "] newState=[" + state.getName() + "]");
      changeListener.l2StateChanged(new StateChangedEvent(serverState, state));
    }

    serverState = state;
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  public synchronized State getState() {
    return serverState;
  }

  private boolean validateState(State state) {
    for (int i = 0; i < StateManager.validStates.length; i++) {
      if (StateManager.validStates[i].equals(state)) { return true; }
    }
    return false;
  }

  public void l2StateChanged(StateChangedEvent sce) {
    setState(sce.getCurrentState());
  }

  public boolean isActiveCoordinator() {
    if (getState().equals(StateManager.ACTIVE_COORDINATOR)) { return true; }
    return false;
  }

  public boolean isPassiveUninitialized() {
    if (getState().equals(StateManager.PASSIVE_UNINTIALIZED)) { return true; }
    return false;
  }

  public boolean isPassiveStandby() {
    if (getState().equals(StateManager.PASSIVE_STANDBY)) { return true; }
    return false;
  }

  public boolean isStartState() {
    if (getState().equals(StateManager.START_STATE)) { return true; }
    return false;
  }

  public boolean isStopState() {
    if (getState().equals(StateManager.STOP_STATE)) { return true; }
    return false;
  }
  
  public void registerStateChangeListener(StateChangeListener listener) {
    if (changeListener != null) { throw new AssertionError("State change listerer is already set."); }
    changeListener = listener;
  }

  public String toString() {
    return getState().getName();
  }
}
