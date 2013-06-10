/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.collections.ToolkitSortedMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.terracotta.toolkit.collections.map.SubTypeWrapperCollection;
import com.terracotta.toolkit.collections.map.SubTypeWrapperSet;
import com.terracotta.toolkit.collections.map.SubTypeWrapperSortedMap;
import com.terracotta.toolkit.collections.map.ToolkitSortedMapImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitMap;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class DestroyableToolkitSortedMap<K extends Comparable<? super K>, V> extends
    AbstractDestroyableToolkitObject<ToolkitSortedMap> implements ToolkitSortedMap<K, V>, RejoinAwareToolkitMap<K, V> {

  private final String                                              name;
  private volatile ToolkitSortedMap<K, V>                           map;
  private final IsolatedClusteredObjectLookup<ToolkitSortedMapImpl> lookup;

  public DestroyableToolkitSortedMap(ToolkitObjectFactory<ToolkitSortedMap> factory,
                                     IsolatedClusteredObjectLookup<ToolkitSortedMapImpl> lookup,
                                     ToolkitSortedMapImpl<K, V> map, String name) {
    super(factory);
    this.lookup = lookup;
    this.map = map;
    this.name = name;
    map.setApplyDestroyCallback(getDestroyApplicator());
  }

  @Override
  public void doRejoinStarted() {
    this.map = ToolkitInstanceProxy.newRejoinInProgressProxy(name, ToolkitSortedMap.class);
  }

  @Override
  public void doRejoinCompleted() {
    if (!isDestroyed()) {
      ToolkitSortedMapImpl afterRejoin = lookup.lookupClusteredObject(name, ToolkitObjectType.SORTED_MAP, null);
      if (afterRejoin == null) {
        destroyApplicator.applyDestroy();
      } else {
        this.map = afterRejoin;
      }
    }
  }

  @Override
  public void applyDestroy() {
    // status.setDestroyed() is called from Parent class
    this.map = ToolkitInstanceProxy.newDestroyedInstanceProxy(name, ToolkitSortedMap.class);
  }

  @Override
  public void doDestroy() {
    map.destroy();
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock() {
    return map.getReadWriteLock();
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return map.get(key);
  }

  @Override
  public V put(K key, V value) {
    return map.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    map.putAll(m);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Set<K> keySet() {
    return new SubTypeWrapperSet<K>(map.keySet(), status, this.name, ToolkitObjectType.SORTED_MAP);
  }

  @Override
  public Collection<V> values() {
    return new SubTypeWrapperCollection<V>(map.values(), status, this.name, ToolkitObjectType.SORTED_MAP);
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new SubTypeWrapperSet<Entry<K, V>>(map.entrySet(), status, this.name, ToolkitObjectType.SORTED_MAP);
  }

  @Override
  public Comparator<? super K> comparator() {
    return map.comparator();
  }

  @Override
  public K firstKey() {
    return map.firstKey();
  }

  @Override
  public K lastKey() {
    return map.lastKey();
  }

  @Override
  public SortedMap<K, V> headMap(K toKey) {
    return new SubTypeWrapperSortedMap<K, V>(map.headMap(toKey), status, this.name, ToolkitObjectType.SORTED_MAP);
  }

  @Override
  public SortedMap<K, V> subMap(K fromKey, K toKey) {
    return new SubTypeWrapperSortedMap<K, V>(map.subMap(fromKey, toKey), status, this.name,
                                             ToolkitObjectType.SORTED_MAP);
  }

  @Override
  public SortedMap<K, V> tailMap(K fromKey) {
    return new SubTypeWrapperSortedMap<K, V>(map.tailMap(fromKey), status, this.name, ToolkitObjectType.SORTED_MAP);
  }

  @Override
  public V putIfAbsent(K paramK, V paramV) {
    return map.putIfAbsent(paramK, paramV);
  }

  @Override
  public boolean replace(K paramK, V paramV1, V paramV2) {
    return map.replace(paramK, paramV1, paramV2);
  }

  @Override
  public V replace(K paramK, V paramV) {
    return map.replace(paramK, paramV);
  }

  @Override
  public boolean remove(Object paramObject1, Object paramObject2) {
    return map.remove(paramObject1, paramObject2);
  }

}
