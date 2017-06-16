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

import org.terracotta.entity.StateDumpCollector;
/**
 *
 */
public class ToStringStateDumpCollector implements StateDumpCollector {

  private final StringBuilder builder = new StringBuilder();
  private final Map<String, ToStringStateDumpCollector> instances = new HashMap<>();
  private final Map<String, String> dumpState = new HashMap<>();

  private final String name;

  public ToStringStateDumpCollector(String name) {
    this(name, null);
  }

  public ToStringStateDumpCollector(String name, ToStringStateDumpCollector parent) {
    if(parent != null) {
      this.name = parent.getName() + NAMESPACE_DELIMITER + name;
    } else {
      this.name = name;
    }
  }

  @Override
  public StateDumpCollector subStateDumpCollector(String name) {
    instances.putIfAbsent(name, new ToStringStateDumpCollector(name, this));
    return instances.get(name);
  }

  @Override
  public void addState(String key, String value) {
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

    for (ToStringStateDumpCollector substate : instances.values()) {
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
