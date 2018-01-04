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

import static com.tc.l2.state.StateManager.ACTIVE_COORDINATOR;
import static com.tc.l2.state.StateManager.PASSIVE_STANDBY;
import static com.tc.l2.state.StateManager.PASSIVE_SYNCING;
import static com.tc.l2.state.StateManager.PASSIVE_UNINITIALIZED;
import static com.tc.l2.state.StateManager.RECOVERING_STATE;
import static com.tc.l2.state.StateManager.START_STATE;
import static com.tc.l2.state.StateManager.STOP_STATE;
import com.tc.util.State;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 */
public enum ServerMode {
  START(START_STATE),
  UNINITIALIZED(PASSIVE_UNINITIALIZED),    
  RECOVERING(RECOVERING_STATE),
  SYNCING(PASSIVE_SYNCING),
  PASSIVE(PASSIVE_STANDBY),
  ACTIVE(ACTIVE_COORDINATOR),
  STOP(STOP_STATE);

  private final State name;

  private ServerMode(State name) {
    this.name = name;
  }

  public State getState() {
    return name;
  }

  @Override
  public String toString() {
    return name.toString();
  }

  public String getName() {
    return name.getName();
  }
  
  public boolean equals() {
    throw new AssertionError();
  }
  
  public static final Set<ServerMode> VALID_STATES = EnumSet.allOf(ServerMode.class);
  public static final Set<ServerMode> PASSIVE_STATES = EnumSet.of(UNINITIALIZED, PASSIVE, SYNCING);
};
