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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class JsonStateDumpCollector implements StateDumpCollector {
  private static final Logger LOGGER = TCLogging.getDumpLogger();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final ConcurrentMap<String, JsonStateDumpCollector> instances = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, String> dumpState = new ConcurrentHashMap<>();
  private String stateInJson;

  private final String name;

  public JsonStateDumpCollector(String name) {
    this.name = name;
  }

  @Override
  public StateDumpCollector subStateDumpCollector(String name) {
    instances.putIfAbsent(name, new JsonStateDumpCollector(name));
    return instances.get(name);
  }

  @Override
  public void addState(String key, String value) {
    if(JSON_STATE_KEY.equals(key)) {
      stateInJson = value;
    } else {
      dumpState.put(key, value);
    }
  }

  public String getName() {
    return name;
  }

  public JsonNode getState() throws Exception {
    if(stateInJson != null) {
      return MAPPER.readTree(stateInJson);
    } else {
      ObjectNode componentState = MAPPER.createObjectNode();
      if(dumpState.size() == 0 && instances.size() == 0) {
          componentState.put("-- cluster state is not available for this component--", "");
      } else {
        for (Map.Entry<String, String> entry : dumpState.entrySet()) {
          componentState.put(entry.getKey(), entry.getValue());
        }

        for (JsonStateDumpCollector jsonStateDumpCollector : instances.values()) {
          componentState.set(jsonStateDumpCollector.getName(), jsonStateDumpCollector.getState());
        }
      }
      return componentState;
    }
  }
}
