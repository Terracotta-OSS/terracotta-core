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
package com.tc.l2.context;

import com.tc.l2.state.StateManager;
import com.tc.util.State;

public class StateChangedEvent {

  private final State from;
  private final State to;

  public StateChangedEvent(State from, State to) {
    this.from = from;
    this.to = to;
  }

  public boolean movedToActive() {
    return StateManager.ACTIVE_COORDINATOR.equals(to);
  }

  public boolean movedToStop() {
    return StateManager.STOP_STATE.equals(to);
  }
  
  public State getCurrentState() {
    return to;
  }
  
  public State getOldState() {
    return from;
  }

  @Override
  public String toString() {
    return "StateChangedEvent [ " + from + " - > " + to + " ]";
  }

}
