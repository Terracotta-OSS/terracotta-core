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
package org.terracotta.entity.map;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.map.common.BooleanResponse;
import org.terracotta.entity.map.common.ClearOperation;
import org.terracotta.entity.map.common.ConditionalRemoveOperation;
import org.terracotta.entity.map.common.ConditionalReplaceOperation;
import org.terracotta.entity.map.common.ContainsKeyOperation;
import org.terracotta.entity.map.common.ContainsValueOperation;
import org.terracotta.entity.map.common.EntrySetOperation;
import org.terracotta.entity.map.common.EntrySetResponse;
import org.terracotta.entity.map.common.GetOperation;
import org.terracotta.entity.map.common.KeySetOperation;
import org.terracotta.entity.map.common.KeySetResponse;
import org.terracotta.entity.map.common.MapOperation;
import org.terracotta.entity.map.common.MapResponse;
import org.terracotta.entity.map.common.MapValueResponse;
import org.terracotta.entity.map.common.PutAllOperation;
import org.terracotta.entity.map.common.PutIfAbsentOperation;
import org.terracotta.entity.map.common.PutIfPresentOperation;
import org.terracotta.entity.map.common.PutOperation;
import org.terracotta.entity.map.common.RemoveOperation;
import org.terracotta.entity.map.common.SizeOperation;
import org.terracotta.entity.map.common.SizeResponse;
import org.terracotta.entity.map.common.ValueCollectionResponse;
import org.terracotta.entity.map.common.ValuesOperation;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.terracotta.entity.InvocationCallback;

import static org.terracotta.entity.map.ValueCodecFactory.getCodecForClass;

@SuppressWarnings("unchecked")
public class TerracottaClusteredMap<K, V> implements ConcurrentClusteredMap<K, V> {

  private final EntityClientEndpoint<MapOperation, MapResponse> endpoint;

  private Class<K> keyClass;
  private Class<V> valueClass;
  private ValueCodec<K> keyValueCodec;
  private ValueCodec<V> valueValueCodec;

  public TerracottaClusteredMap(EntityClientEndpoint<MapOperation, MapResponse> endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public void setTypes(Class<K> keyClass, Class<V> valueClass) {
    this.keyClass = keyClass;
    this.valueClass = valueClass;
    keyValueCodec = getCodecForClass(keyClass);
    valueValueCodec = getCodecForClass(valueClass);
  }

  @Override
  public void close() {
    this.endpoint.close();
  }

  @Override
  public int size() {
    Long size = ((SizeResponse)invokeWithReturn(new SizeOperation())).getSize();
    if (size > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else if (size <= 0) {
      return 0;
    } else {
      return size.intValue();
    }
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    if (!keyClass.isAssignableFrom(key.getClass())) {
      return false;
    }
    return ((BooleanResponse)invokeWithReturn(new ContainsKeyOperation(keyValueCodec.encode((K)key)))).isTrue();
  }

  @Override
  public boolean containsValue(Object value) {
    if (!valueClass.isAssignableFrom(value.getClass())) {
      return false;
    }
    return ((BooleanResponse)invokeWithReturn(new ContainsValueOperation(valueValueCodec.encode((V) value)))).isTrue();
  }

  @Override
  public V get(Object key) {
    if (!keyClass.isAssignableFrom(key.getClass())) {
      return null;
    }
    MapValueResponse response = (MapValueResponse) invokeWithReturn(new GetOperation(keyValueCodec.encode((K) key)));
    return valueValueCodec.decode(response.getValue());
  }

  @Override
  public V put(K key, V value) {
    MapValueResponse response = (MapValueResponse) invokeWithReturn(new PutOperation(keyValueCodec.encode(key), valueValueCodec.encode(value)));
    return valueValueCodec.decode(response.getValue());
  }
  
  @Override
  public void insert(K key, V value) {
    fireAndForget(new PutOperation(keyValueCodec.encode(key), valueValueCodec.encode(value)));
  }

  @Override
  public V remove(Object key) {
    if (!keyClass.isAssignableFrom(key.getClass())) {
      return null;
    }
    MapValueResponse mapValueResponse = (MapValueResponse) invokeWithReturn(new RemoveOperation(keyValueCodec.encode((K) key)));
    return valueValueCodec.decode(mapValueResponse.getValue());
  }
  
  private void fireAndForget(MapOperation operation) {
    try {
      endpoint.message(operation)
          .invokeAnd(InvocationCallback.Types.SENT).get();
    } catch (Exception e) {
      throw new RuntimeException("Exception while processing map operation " + operation, e);
    }
  }

  private MapResponse invokeWithReturn(MapOperation operation) {
    try {
      return endpoint.message(operation)
          .invoke()
          .get();
    } catch (Exception e) {
      throw new RuntimeException("Exception while processing map operation " + operation, e);
    }
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    HashMap<Object, Object> input = new HashMap<Object, Object>();
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      input.put(keyValueCodec.encode(entry.getKey()), valueValueCodec.encode(entry.getValue()));
    }
    invokeWithReturn(new PutAllOperation(input));
  }

  @Override
  public void clear() {
    invokeWithReturn(new ClearOperation());
  }

  @Override
  public Set<K> keySet() {
    Set<K> result = new HashSet<K>();
    KeySetResponse keySetResponse = (KeySetResponse) invokeWithReturn(new KeySetOperation());
    for (Object o : keySetResponse.getKeySet()) {
      result.add(keyValueCodec.decode(o));
    }
    return result;
  }

  @Override
  public Collection<V> values() {
    ArrayList<V> result = new ArrayList<V>();
    ValueCollectionResponse response = (ValueCollectionResponse) invokeWithReturn(new ValuesOperation());
    for (Object o : response.getValues()) {
      result.add(valueValueCodec.decode(o));
    }
    return result;
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    HashSet<Entry<K, V>> result = new HashSet<Entry<K, V>>();
    EntrySetResponse response = (EntrySetResponse) invokeWithReturn(new EntrySetOperation());
    for (Entry<Object, Object> entry : response.getEntrySet()) {
      result.add(new AbstractMap.SimpleEntry<K, V > (keyValueCodec.decode(entry.getKey()), valueValueCodec.decode(entry.getValue())));
    }
    return result;
  }

  @Override
  public V putIfAbsent(K key, V value) {
    MapValueResponse response = (MapValueResponse) invokeWithReturn(new PutIfAbsentOperation(keyValueCodec.encode(key), valueValueCodec.encode(value)));
    return valueValueCodec.decode(response.getValue());
  }

  @Override
  public boolean remove(Object key, Object value) {
    if (!keyClass.isAssignableFrom(key.getClass())) {
      return false;
    }
    if (!valueClass.isAssignableFrom(value.getClass())) {
      return false;
    }
    MapOperation operation = new ConditionalRemoveOperation(keyValueCodec.encode((K) key), valueValueCodec.encode((V) value));
    return ((BooleanResponse) invokeWithReturn(operation)).isTrue();
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    MapOperation operation = new ConditionalReplaceOperation(keyValueCodec.encode(key), valueValueCodec.encode(oldValue), valueValueCodec.encode(newValue));
    return ((BooleanResponse) invokeWithReturn(operation)).isTrue();
  }

  @Override
  public V replace(K key, V value) {
    MapOperation operation = new PutIfPresentOperation(keyValueCodec.encode(key), valueValueCodec.encode(value));
    MapValueResponse response = (MapValueResponse) invokeWithReturn(operation);
    return valueValueCodec.decode(response.getValue());
  }
}
