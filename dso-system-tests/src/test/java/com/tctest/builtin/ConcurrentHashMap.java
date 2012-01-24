/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.builtin;

import com.tc.util.Util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentHashMap<K, V> implements ConcurrentMap<K, V> {
  private final List<Lock>      locks;
  private final List<Map<K, V>> segments;

  public ConcurrentHashMap() {
    this(16);
  }

  public ConcurrentHashMap(int concurrency) {
    this.locks = new ArrayList<Lock>(concurrency);
    this.segments = new ArrayList<Map<K, V>>();

    for (int i = 0; i < concurrency; i++) {
      locks.add(new Lock());
      segments.add(new HashMap<K, V>());
    }
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsKey(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V get(Object key) {
    int index = indexFor(key);
    Lock lock = locks.get(index);
    lock.readLock();
    try {
      return segments.get(index).get(key);
    } finally {
      lock.readUnlock();
    }
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmpty() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<K> keySet() {
    throw new UnsupportedOperationException();
  }

  public V put(K key, V value) {
    int index = indexFor(key);
    Lock lock = locks.get(index);
    lock.writeLock();
    try {
      return segments.get(index).put(key, value);
    } finally {
      lock.writeUnlock();
    }
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException();
  }

  public V putIfAbsent(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V remove(Object key) {
    int index = indexFor(key);
    Lock lock = locks.get(index);
    lock.writeLock();
    try {
      return segments.get(index).remove(key);
    } finally {
      lock.writeUnlock();
    }
  }

  @Override
  public boolean remove(Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  public boolean replace(K key, V oldValue, V newValue) {
    throw new UnsupportedOperationException();
  }

  public V replace(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<V> values() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    throw new UnsupportedOperationException();
  }

  private int indexFor(Object key) {
    return Util.hash(key, segments.size());
  }
}
