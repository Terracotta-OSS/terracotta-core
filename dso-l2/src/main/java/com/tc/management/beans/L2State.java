/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
