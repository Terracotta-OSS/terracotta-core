/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.entity.map.server;

import java.util.Map;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.map.common.MapOperation;
import org.terracotta.entity.map.common.MapResponse;

import org.terracotta.entity.map.common.ConditionalRemoveOperation;
import org.terracotta.entity.map.common.ConditionalReplaceOperation;
import org.terracotta.entity.map.common.PutAllOperation;
import org.terracotta.entity.map.common.PutIfAbsentOperation;
import org.terracotta.entity.map.common.PutIfPresentOperation;
import org.terracotta.entity.map.common.PutOperation;
import org.terracotta.entity.map.common.RemoveOperation;

/**
 * PassiveTerracottaClusteredMap
 */
class PassiveTerracottaClusteredMap implements PassiveServerEntity<MapOperation, MapResponse> {

  private final String name;
  private final Map<String, CompoundMap<Object, Object>> root;
  private final CompoundMap<Object, Object> map;

  public PassiveTerracottaClusteredMap(String name, int concurrency, Map<String, CompoundMap<Object, Object>> root) {
    this.name = name;
    this.root = root;
    this.map = root.computeIfAbsent(name, v -> new CompoundMap<>(concurrency));
  }

  @Override
  public void invokePassive(InvokeContext context, MapOperation input) {
    switch (input.operationType()) {
      case PUT: {
        PutOperation putOperation = (PutOperation) input;
        Object key = putOperation.getKey();
        Object old = map.get(key);
        map.put(key, putOperation.getValue());
        break;
      }
      case REMOVE: {
        Object key = ((RemoveOperation) input).getKey();
        break;
      }
      case CLEAR: {
        map.clear();
        // There is no response from the clear.
        break;
      }
      case PUT_ALL: {
        @SuppressWarnings("unchecked")
        Map<Object, Object> newValues = (Map<Object, Object>) ((PutAllOperation)input).getMap();
        map.putAll(newValues);
        // There is no response from a put all.
        break;
      }
      case PUT_IF_ABSENT: {
        PutIfAbsentOperation operation = (PutIfAbsentOperation) input;
        map.putIfAbsent(operation.getKey(), operation.getValue());
        break;
      }
      case PUT_IF_PRESENT: {
        PutIfPresentOperation operation = (PutIfPresentOperation) input;
        map.replace(operation.getKey(), operation.getValue());
        break;
      }
      case CONDITIONAL_REMOVE: {
        ConditionalRemoveOperation operation = (ConditionalRemoveOperation) input;
        map.remove(operation.getKey(), operation.getValue());
        break;
      }
      case CONDITIONAL_REPLACE: {
        ConditionalReplaceOperation operation = (ConditionalReplaceOperation) input;
        map.replace(operation.getKey(), operation.getOldValue(), operation.getNewValue());
        break;
      }
      case SYNC_OP: {
        SyncOperation op = (SyncOperation)input;
        map.putMapForSegment(op.getConcurrency(), op.getObjectMap());
        break;
      }
      default:
        // Unknown message type.
        throw new AssertionError("Unsupported message type: " + input.operationType());
    }
  }

  @Override
public void startSyncEntity() {

  }

  @Override
public void endSyncEntity() {

  }

  @Override
public void startSyncConcurrencyKey(int concurrencyKey) {

}

  @Override
public void endSyncConcurrencyKey(int concurrencyKey) {

}

  @Override
public void createNew() {

  }

  @Override
public void destroy() {
    root.remove(name);
  }
}
