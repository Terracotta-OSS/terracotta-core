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

import java.util.LinkedHashMap;
import java.util.Map;

import org.terracotta.entity.StateDumpCollector;
/**
 *
 */
public class MappedStateCollector implements StateDumpCollector {

  private final Map<String, Object> dumpState = new LinkedHashMap<>();
  private final String name;

  public MappedStateCollector(String name) {
    this.name = name;
  }

  @Override
  public StateDumpCollector subStateDumpCollector(String name) {
    MappedStateCollector sub = new MappedStateCollector(name);
    dumpState.put(name, sub.getMap());
    return sub;
  }

  @Override
  public void addState(String key, String value) {
    dumpState.put(key, value);
  }

  public String getName() {
    return name;
  }
  
  public Map<String, Object> getMap() {
    return dumpState;
  }
}
