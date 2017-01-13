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
package com.tc.services;

import java.util.HashMap;
import java.util.Map;
import org.terracotta.entity.StateDumper;
import static org.terracotta.entity.StateDumper.NAMESPACE_DELIMITER;

/**
 *
 */
public class ToStringStateDumper implements StateDumper {

  private final StringBuilder builder = new StringBuilder();
  private final Map<String, ToStringStateDumper> instances = new HashMap<>();
  private final Map<String, String> dumpState = new HashMap<>();

  private final String name;

  public ToStringStateDumper(String name) {
    this(name, null);
  }

  public ToStringStateDumper(String name, ToStringStateDumper parent) {
    if(parent != null) {
      this.name = parent.getName() + NAMESPACE_DELIMITER + name;
    } else {
      this.name = name;
    }
  }

  @Override
  public StateDumper subStateDumper(String name) {
    instances.putIfAbsent(name, new ToStringStateDumper(name, this));
    return instances.get(name);
  }

  @Override
  public void dumpState(String key, String value) {
    dumpState.put(key, value);
  }

  public String getName() {
    return name;
  }

  public void logState() {
    if(dumpState.size() > 0) {
      builder.append("\n***********************************************************************************\n");
      builder.append(name + " state ");
      for (Map.Entry<String, String> entry : dumpState.entrySet()) {
        builder.append(entry.getKey() + "=" + entry.getValue());
      }
    }

    for (ToStringStateDumper substate : instances.values()) {
      substate.logState();
      builder.append(substate.toString());
    }
    
    instances.clear();
    dumpState.clear();
  }

  @Override
  public String toString() {
    logState();
    return builder.toString();
  }
}
