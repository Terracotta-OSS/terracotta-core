/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Stage;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class StageController {

  private final Map<com.tc.util.State, Set<String>> states = new HashMap<com.tc.util.State, Set<String>>();

  public StageController() {
  }

  public synchronized void addStageToState(com.tc.util.State state, String stage) {
    Set<String> list = states.get(state);
    if (list == null) {
      list = new HashSet<String>();
      states.put(state, list);
    }
    list.add(stage);
  }

  public synchronized void removeStageFromState(com.tc.util.State state, String stage) {
    Set<String> list = states.get(state);
    if (list != null) {
      list.remove(stage);
    }
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
      Stage<?> st = cxt.getStage(s, Object.class);
      if (st != null && !leaving.contains(s)) {
        st.start(cxt);
        st.unpause();
      }
    }
  }
}
