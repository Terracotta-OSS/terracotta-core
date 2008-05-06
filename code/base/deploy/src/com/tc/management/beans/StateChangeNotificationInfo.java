/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.l2.state.StateManager;
import com.tc.util.State;

import java.util.HashMap;
import java.util.Map;

public class StateChangeNotificationInfo {
  // order matters: msg, attribute name, attribute type
  private static final String[] ACTIVE_INFO          = new String[] { "TCServer active", "Active",
      "jmx.terracotta.L2.active"                    };
  private static final String[] PASSIVE_UNINIT_INFO  = new String[] { "TCServer passive-uninitialized",
      "Paasive-Uninitialized", "jmx.terracotta.L2.passive-uninitialized" };
  private static final String[] PASSIVE_STANDBY_INFO = new String[] { "TCServer passive-standby", "Passive-Standby",
      "jmx.terracotta.L2.passive-standby"           };
  private static final String[] START_INFO           = new String[] { "TCServer start-state", "Start-State",
      "jmx.terracotta.L2.start-state"               };
  private static final String[] STOP_INFO            = new String[] { "TCServer stop-state", "Stop-State",
      "jmx.terracotta.L2.stop-state"                };

  private final Map             map;

  public StateChangeNotificationInfo() {
    map = new HashMap();
    map.put(StateManager.ACTIVE_COORDINATOR.getName(), ACTIVE_INFO);
    map.put(StateManager.PASSIVE_STANDBY.getName(), PASSIVE_STANDBY_INFO);
    map.put(StateManager.PASSIVE_UNINTIALIZED.getName(), PASSIVE_UNINIT_INFO);
    map.put(StateManager.START_STATE.getName(), START_INFO);
    map.put(StateManager.STOP_STATE.getName(), STOP_INFO);
  }

  public String getMsg(State state) {
    String[] info = (String[]) map.get(state.getName());
    return info[0];
  }

  public String getAttributeName(State state) {
    String[] info = (String[]) map.get(state.getName());
    return info[1];
  }

  public String getAttributeType(State state) {
    String[] info = (String[]) map.get(state.getName());
    return info[2];
  }

}
