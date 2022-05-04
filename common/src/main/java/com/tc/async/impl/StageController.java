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
import com.tc.util.State;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class StageController {

  private static Logger LOGGER = LoggerFactory.getLogger(StageController.class);
  private final Map<com.tc.util.State, Set<String>> states = new HashMap<>();
  private final Map<String, Consumer<State>> triggers = new HashMap<>();
  private final Supplier<ConfigurationContext> context;

  public StageController(Supplier<ConfigurationContext> getter) {
    this.context = getter;
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
      list = new LinkedHashSet<>();
      states.put(state, list);
    }
    list.add(stage);
  }
  /**
   * Adds a runnable trigger to the state transition map
   * @param state
   * @param trigger 
   */
  public synchronized void addTriggerToState(com.tc.util.State state, Consumer<State> trigger) {
    Set<String> list = states.get(state);
    String uuid = "TRIGGER-" + UUID.randomUUID().toString();
    triggers.put(uuid, trigger);
    if (list == null) {
// order is important here.  each stage should be started in the order added.
      list = new LinkedHashSet<>();
      states.put(state, list);
    }
    list.add(uuid);
  }

  public void transition(com.tc.util.State old, com.tc.util.State current) {
    ConfigurationContext cxt = this.context.get();
    Set<String> leaving = states.get(old);
    Set<String> coming = states.get(current);
    if (leaving == null) {
      leaving = Collections.emptySet();
    }
    if (coming == null) {
      coming = Collections.emptySet();
    }
    for (String s : leaving) {
      try {
        Stage<?> st = cxt.getStage(s, Object.class);
        if (st != null && !coming.contains(s)) {
          st.destroy();
        }
      } catch (Exception e) {
        LOGGER.error("failed to destroy stage {}", s);
        throw new RuntimeException(e);
      }
    }
    for (String s : coming) {
      if (s.startsWith("TRIGGER-")) {
        triggers.get(s).accept(old);
      } else {
        try {
          Stage<?> st = cxt.getStage(s, Object.class);
          if (st != null && !leaving.contains(s) && !st.isStarted()) {
            st.start(cxt);
            st.unpause();
          }
        } catch (Exception e) {
          LOGGER.error("failed to start stage {}", s);
          throw new RuntimeException(e);
        }
      }
    }
  }
}
