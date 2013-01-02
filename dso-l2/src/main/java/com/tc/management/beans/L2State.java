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
    return StateManager.validStates.contains(state);
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    setState(sce.getCurrentState());
  }

  public boolean isActiveCoordinator() {
    return getState().equals(StateManager.ACTIVE_COORDINATOR);
  }

  public boolean isPassiveUninitialized() {
    return getState().equals(StateManager.PASSIVE_UNINITIALIZED);
  }

  public boolean isPassiveStandby() {
    return getState().equals(StateManager.PASSIVE_STANDBY);
  }

  public boolean isStartState() {
    return getState().equals(StateManager.START_STATE);
  }

  public boolean isStopState() {
    return getState().equals(StateManager.STOP_STATE);
  }

  public boolean isRecovering() {
    return getState().equals(StateManager.RECOVERING);
  }

  public void registerStateChangeListener(StateChangeListener listener) {
    if (changeListener != null) { throw new AssertionError("State change listerer is already set."); }
    changeListener = listener;
  }

  @Override
  public String toString() {
    return getState().getName();
  }
}
