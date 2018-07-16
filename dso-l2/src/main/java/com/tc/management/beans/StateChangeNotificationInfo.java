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
package com.tc.management.beans;

import com.tc.l2.state.ServerMode;
import java.util.EnumMap;

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
  private static final String[] RECOVERING_INFO        = new String[] { "TCServer recovering", "Recovering",
      "jmx.terracotta.L2.recovering"                };


  private static final Map<ServerMode, String[]>             map;
  static {
    map = new EnumMap<>(ServerMode.class);
    map.put(ServerMode.ACTIVE, ACTIVE_INFO);
    map.put(ServerMode.PASSIVE, PASSIVE_STANDBY_INFO);
    map.put(ServerMode.UNINITIALIZED, PASSIVE_UNINIT_INFO);
    map.put(ServerMode.START, START_INFO);
    map.put(ServerMode.STOP, STOP_INFO);
    map.put(ServerMode.RECOVERING, RECOVERING_INFO);
  }

  public String getMsg(ServerMode state) {
    String[] info = map.get(state);
    return info[0];
  }

  public String getAttributeName(ServerMode state) {
    String[] info = map.get(state);
    return info[1];
  }

  public String getAttributeType(ServerMode state) {
    String[] info = map.get(state);
    return info[2];
  }

}
