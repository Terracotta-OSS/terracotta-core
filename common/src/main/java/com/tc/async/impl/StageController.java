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
package com.tc.async.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Stage;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *
 */
public class StageController {

  private final Map<com.tc.util.State, Set<String>> states = new HashMap<com.tc.util.State, Set<String>>();
  private final Map<String, Runnable> triggers = new HashMap<String, Runnable>();

  public StageController() {
  }
/**
 * makes sure a stage is running when the server transitions to a state
 * @param state state where stage should be active
 * @param stage name of the stage to be active
 */
  public synchronized void addStageToState(com.tc.util.State state, String stage) {
    Set<String> list = states.get(state);
    if (list == null) {
// order is important here.  each stage should be started in the order added.
      list = new LinkedHashSet<String>();
      states.put(state, list);
    }
    list.add(stage);
  }
  /**
   * Adds a runnable trigger to the state transition map
   * @param state
   * @param trigger 
   */
  public synchronized void addTriggerToState(com.tc.util.State state, Runnable trigger) {
    Set<String> list = states.get(state);
    String uuid = "TRIGGER-" + UUID.randomUUID().toString();
    triggers.put(uuid, trigger);
    if (list == null) {
// order is important here.  each stage should be started in the order added.
      list = new LinkedHashSet<String>();
      states.put(state, list);
    }
    list.add(uuid);
  }

  public void transition(ConfigurationContext cxt, com.tc.util.State old, com.tc.util.State current) {
    Set<String> leaving = states.get(old);
    Set<String> coming = states.get(current);
    if (leaving == null) {
      leaving = Collections.emptySet();
    }
    if (coming == null) {
      coming = Collections.emptySet();
    }
    for (String s : leaving) {
      Stage<?> st = cxt.getStage(s, Object.class);
      if (st != null && !coming.contains(s)) {
        st.destroy();
      }
    }
    for (String s : coming) {
      if (s.startsWith("TRIGGER-")) {
        triggers.get(s).run();
      } else {
        Stage<?> st = cxt.getStage(s, Object.class);
        if (st != null && !leaving.contains(s)) {
          st.start(cxt);
          st.unpause();
        }
      }
    }
  }
}
