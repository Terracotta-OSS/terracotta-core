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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class CompoundMap<K, V> implements Map<K, V> {
  
  private final List<Map<K,V>> segments;
  private static final int MIXER = 31;

  public CompoundMap(int concurrency) {
    this.segments = new ArrayList<>(concurrency);
    for (int x=0;x<concurrency;x++) {
      segments.add(new HashMap<>());
    }
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return segments.stream().flatMap(seg->seg.entrySet().stream()).collect(Collectors.toSet());
  }

  @Override
  public int size() {
    return segments.stream().map(Map::size).reduce(0, (a, m)->a+m);
  }

  @Override
  public boolean isEmpty() {
    return segments.stream().allMatch(Map::isEmpty);
  }

  @Override
  public boolean containsKey(Object key) {
    return segment(key).containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return segments.stream().anyMatch(m->m.containsValue(value));
  }

  @Override
  public V get(Object key) {
    return segment(key).get(key);
  }

  @Override
  public V put(K key, V value) {
    return segment(key).put(key, value);
  }

  @Override
  public V remove(Object key) {
    return segment(key).remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    m.entrySet().stream().forEach(e->put(e.getKey(), e.getValue()));
  }

  @Override
  public void clear() {
    segments.stream().forEach(Map::clear);
  }

  @Override
  public Set<K> keySet() {
    return segments.stream().flatMap(m->m.keySet().stream()).collect(Collectors.toSet());
  }

  @Override
  public Collection<V> values() {
    return segments.stream().flatMap(m->m.values().stream()).collect(Collectors.toList());
  }
  
  private Map<K, V> segment(Object key) {
    return segments.get(segment(key, segments.size()));
  }
  
  public static int segment(Object key, int segments) {
    return Math.abs(key.hashCode() * MIXER) % segments;
  }
  
  public Map<K, V> mapForSegment(int seg) {
    if (seg < 1 || seg > segments.size()) {
      throw new IllegalArgumentException();
    }
    return segments.get(seg - 1);
  }
  
  public Map<K, V> putMapForSegment(int seg, Map map) {
    if (seg < 1 || seg > segments.size()) {
      throw new IllegalArgumentException();
    }
    return segments.set(seg - 1, map);
  }
  
  public int concurrency() {
    return segments.size();
  }
}
