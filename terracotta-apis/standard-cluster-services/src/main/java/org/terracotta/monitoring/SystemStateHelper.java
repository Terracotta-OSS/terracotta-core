/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law ord agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.monitoring;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class SystemStateHelper {
  private final Map<String, ?> state;

  private static final String[] CONNECTIONS = {"CLIENT_COMMS", "connectionManager", "connections"};
  private static final String[] PEERS = {"GROUP_COMMS", "connectionManager", "connections"};
  private static final String[] VOTERS = {"COORDINATOR", "stateManager", "consistency", "delegate", "voter", "count"};
  private static final String[] INVOKE_COUNT = {"ENTITY_MANAGER", "stateManager", "consistency", "delegate", "voter", "invokeCount"};

  public SystemStateHelper(Map<String, ?> state) {
    this.state = state;
  }

  public long getClientCount() {
    return getList(CONNECTIONS).map(List::size).orElse(0);
  }

  public long getPeerCount() {
    return getList(PEERS).map(List::size).orElse(0);
  }

  public long getVoterCount() {
    return getNumber(VOTERS).orElse(0).longValue();
  }

  public long getInvokeCount() {
    return getNumber(INVOKE_COUNT).orElse(0).longValue();
  }

  private Optional<List<?>> getList(String[] path) {
    if (getPath(path) instanceof List l) {
      return Optional.of(l);
    }
    return Optional.empty();
  }

  private Optional<Number> getNumber(String[] path) {
    if (getPath(path) instanceof Number n) {
      return Optional.of(n);
    }
    return Optional.empty();
  }

  private Object getPath(String[] path) {
    Map<String, ?> target = state;
    Object option = target;
    for (String p : path) {
      option = target.get(p);
      if (option instanceof Map) {
        target = (Map<String, ?>)option;
      } else {
        break;
      }
    }
    return option;
  }
}
