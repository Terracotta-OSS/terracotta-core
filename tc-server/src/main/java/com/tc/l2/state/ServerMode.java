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

import java.util.EnumSet;
import java.util.Set;

import static com.tc.l2.state.StateManager.ACTIVE_COORDINATOR;
import static com.tc.l2.state.StateManager.BOOTSTRAP_STATE;
import static com.tc.l2.state.StateManager.DIAGNOSTIC_STATE;
import static com.tc.l2.state.StateManager.PASSIVE_RELAY;
import static com.tc.l2.state.StateManager.PASSIVE_REPLICA;
import static com.tc.l2.state.StateManager.PASSIVE_STANDBY;
import static com.tc.l2.state.StateManager.PASSIVE_SYNCING;
import static com.tc.l2.state.StateManager.PASSIVE_UNINITIALIZED;
import static com.tc.l2.state.StateManager.RECOVERING_STATE;
import static com.tc.l2.state.StateManager.START_STATE;
import static com.tc.l2.state.StateManager.STOP_STATE;
import com.tc.util.State;

/**
 *
 */
public enum ServerMode {
  INITIAL(BOOTSTRAP_STATE) {
    @Override
    public boolean containsData() {
      return false;
    }

    @Override
    public boolean canBeActive() {
      return true;
    }

    @Override
    public boolean isStartup() {
      return true;
    }
  },
  START(START_STATE) {
    @Override
    public boolean containsData() {
      return false;
    }

    @Override
    public boolean canBeActive() {
      return true;
    }

    @Override
    public boolean isStartup() {
      return true;
    }
  },
  UNINITIALIZED(PASSIVE_UNINITIALIZED) {
    @Override
    public boolean containsData() {
      return false;
    }
  },    
  RECOVERING(RECOVERING_STATE),
  SYNCING(PASSIVE_SYNCING),
  PASSIVE(PASSIVE_STANDBY) {
    @Override
    public boolean canBeActive() {
      return true;
    }

    @Override
    public boolean canStartElection() {
      return true;
    }
  },
  RELAY(PASSIVE_RELAY) {
    @Override
    public boolean isStartup() {
      return true;
    }
    
    @Override
    public boolean canStartElection() {
      return false;
    }

    @Override
    public boolean containsData() {
      return false;
    }

    @Override
    public boolean canBeActive() {
      return false;
    }

    @Override
    public boolean requiresElection() {
      return false;
    }
  },
  REPLICA(PASSIVE_REPLICA) {
    @Override
    public boolean canBeActive() {
      return true;
    }

    @Override
    public boolean canStartElection() {
      return true;
    }
  },
  ACTIVE(ACTIVE_COORDINATOR) {
    @Override
    public boolean canBeActive() {
      return true;
    }
  },
  STOP(STOP_STATE) {
    @Override
    public boolean requiresElection() {
      return false;
    }
  },
  DIAGNOSTIC(DIAGNOSTIC_STATE) {
    @Override
    public boolean requiresElection() {
      return false;
    }
  };

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
  
  public boolean canBeActive() {
    return false;
  }
  
  public boolean containsData() {
    return true;
  }

  public boolean isStartup() {
    return false;
  }

  public boolean canStartElection() {
    return isStartup();
  }
  
  public boolean requiresElection() {
    return true;
  }
  
  public static final Set<ServerMode> VALID_STATES = EnumSet.allOf(ServerMode.class);
  public static final Set<ServerMode> PASSIVE_STATES = EnumSet.of(UNINITIALIZED, PASSIVE, SYNCING, RELAY, REPLICA);
};
