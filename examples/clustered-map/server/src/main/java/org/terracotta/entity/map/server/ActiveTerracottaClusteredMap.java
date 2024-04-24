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

import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.map.common.BooleanResponse;
import org.terracotta.entity.map.common.ConditionalRemoveOperation;
import org.terracotta.entity.map.common.ConditionalReplaceOperation;
import org.terracotta.entity.map.common.ContainsKeyOperation;
import org.terracotta.entity.map.common.ContainsValueOperation;
import org.terracotta.entity.map.common.EntrySetResponse;
import org.terracotta.entity.map.common.GetOperation;
import org.terracotta.entity.map.common.KeySetResponse;
import org.terracotta.entity.map.common.MapOperation;
import org.terracotta.entity.map.common.MapResponse;
import org.terracotta.entity.map.common.MapValueResponse;
import org.terracotta.entity.map.common.NullResponse;
import org.terracotta.entity.map.common.PutAllOperation;
import org.terracotta.entity.map.common.PutIfAbsentOperation;
import org.terracotta.entity.map.common.PutIfPresentOperation;
import org.terracotta.entity.map.common.PutOperation;
import org.terracotta.entity.map.common.RemoveOperation;
import org.terracotta.entity.map.common.SizeResponse;
import org.terracotta.entity.map.common.ValueCollectionResponse;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ActiveTerracottaClusteredMap implements ActiveServerEntity<MapOperation, MapResponse>  {

  private final String name;
  private final Map<String, CompoundMap<Object, Object>> root;
  private final CompoundMap<Object, Object> map;

  public ActiveTerracottaClusteredMap(String name, int concurrency, Map<String, CompoundMap<Object, Object>> root) {
    this.name = name;
    this.root = root;
    this.map = root.computeIfAbsent(name, v->new CompoundMap<>(concurrency));
  }
  
  @Override
  public void connected(ClientDescriptor clientDescriptor) {
  }

  @Override
  public ActiveServerEntity.ReconnectHandler startReconnect() {
    return (ClientDescriptor clientDescriptor, byte[] extendedReconnectData)->{
    // Do nothing.
    };
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
  }

  @Override
  public MapResponse invokeActive(ActiveInvokeContext<MapResponse> context, MapOperation input) {
    MapResponse response;
    
    switch (input.operationType()) {
      case PUT: {
        PutOperation putOperation = (PutOperation) input;
        Object key = putOperation.getKey();
        Object old = map.get(key);
        map.put(key, putOperation.getValue());
        response = new MapValueResponse(old);
        break;
      }
      case GET: {
        Object key = ((GetOperation) input).getKey();
        response = new MapValueResponse(map.get(key));
        break;
      }
      case REMOVE: {
        Object key = ((RemoveOperation) input).getKey();
        response = new MapValueResponse(map.remove(key));
        break;
      }
      case CONTAINS_KEY: {
        Object key = ((ContainsKeyOperation) input).getKey();
        response = new BooleanResponse(map.containsKey(key));
        break;
      }
      case CONTAINS_VALUE: {
        Object value = ((ContainsValueOperation) input).getValue();
        response = new BooleanResponse(map.containsValue(value));
        break;
      }
      case CLEAR: {
        map.clear();
        // There is no response from the clear.
        response = new NullResponse();
        break;
      }
      case PUT_ALL: {
        @SuppressWarnings("unchecked")
        Map<Object, Object> newValues = (Map<Object, Object>) ((PutAllOperation)input).getMap();
        map.putAll(newValues);
        // There is no response from a put all.
        response = new NullResponse();
        break;
      }
      case KEY_SET: {
        Set<Object> keySet = new HashSet<Object>();
        keySet.addAll(map.keySet());
        response = new KeySetResponse(keySet);
        break;
      }
      case VALUES: {
        Collection<Object> values = new ArrayList<Object>();
        values.addAll(map.values());
        response = new ValueCollectionResponse(values);
        break;
      }
      case ENTRY_SET: {
        Set<Map.Entry<Object, Object>> entrySet = new HashSet<Map.Entry<Object, Object>>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          entrySet.add(new AbstractMap.SimpleEntry<Object, Object > (entry.getKey(), entry.getValue()));
        }
        response = new EntrySetResponse(entrySet);
        break;
      }
      case SIZE: {
        response = new SizeResponse(map.size());
        break;
      }
      case PUT_IF_ABSENT: {
        PutIfAbsentOperation operation = (PutIfAbsentOperation) input;
        response = new MapValueResponse(map.putIfAbsent(operation.getKey(), operation.getValue()));
        break;
      }
      case PUT_IF_PRESENT: {
        PutIfPresentOperation operation = (PutIfPresentOperation) input;
        response = new MapValueResponse(map.replace(operation.getKey(), operation.getValue()));
        break;
      }
      case CONDITIONAL_REMOVE: {
        ConditionalRemoveOperation operation = (ConditionalRemoveOperation) input;
        response = new BooleanResponse(map.remove(operation.getKey(), operation.getValue()));
        break;
      }
      case CONDITIONAL_REPLACE: {
        ConditionalReplaceOperation operation = (ConditionalReplaceOperation) input;
        response = new BooleanResponse(map.replace(operation.getKey(), operation.getOldValue(), operation.getNewValue()));
        break;
      }
      default:
        // Unknown message type.
        throw new AssertionError("Unsupported message type: " + input.operationType());
    }
    return response;
  }

  @Override
  public void createNew() {
  }

  @Override
  public void loadExisting() {
  }

  @Override
  public void destroy() {
    map.clear();
    root.remove(name);
  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<MapOperation> syncChannel, int concurrencyKey) {
    syncChannel.synchronizeToPassive(new SyncOperation(concurrencyKey, map.mapForSegment(concurrencyKey)));
  }
}
