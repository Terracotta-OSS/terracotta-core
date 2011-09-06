/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.context;

import com.tc.async.api.EventContext;
import com.tc.l2.state.StateManager;
import com.tc.util.State;

public class StateChangedEvent implements EventContext {

  private final State from;
  private final State to;

  public StateChangedEvent(State from, State to) {
    this.from = from;
    this.to = to;
  }

  public boolean movedToActive() {
    return to == StateManager.ACTIVE_COORDINATOR;
  }
  
  public State getCurrentState() {
    return to;
  }
  
  public State getOldState() {
    return from;
  }

  public String toString() {
    return "StateChangedEvent [ " + from + " - > " + to + " ]";
  }

}
