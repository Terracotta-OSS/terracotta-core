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
