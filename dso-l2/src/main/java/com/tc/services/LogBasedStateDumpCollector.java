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

import org.slf4j.Logger;
import org.terracotta.entity.StateDumpCollector;

import com.tc.logging.TCLogging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A Log based implementation of {@link StateDumpCollector}, key-value mappings will be logged using {@link Logger}
 *
 * @author vmad
 */
public class LogBasedStateDumpCollector implements StateDumpCollector {

  private static final Logger LOGGER = TCLogging.getDumpLogger();
  private final ConcurrentMap<String, LogBasedStateDumpCollector> instances = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, String> dumpState = new ConcurrentHashMap<>();


  private final LogBasedStateDumpCollector parent;
  private final String name;

  public LogBasedStateDumpCollector(String name) {
    this(name, null);
  }

  public LogBasedStateDumpCollector(String name, LogBasedStateDumpCollector parent) {
    if(parent != null) {
      this.name = parent.getName() + NAMESPACE_DELIMITER + name;
    } else {
      this.name = name;
    }
    this.parent = parent;
  }

  @Override
  public StateDumpCollector subStateDumpCollector(String name) {
    instances.putIfAbsent(name, new LogBasedStateDumpCollector(name, this));
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
      LOGGER.info("\n***********************************************************************************\n");
      LOGGER.info(name + " state ");
      for (Map.Entry<String, String> entry : dumpState.entrySet()) {
        LOGGER.info(entry.getKey() + "=" + entry.getValue());
      }
    }

    for (LogBasedStateDumpCollector logBasedStateDumpCollector : instances.values()) {
      logBasedStateDumpCollector.logState();
    }
  }
}
