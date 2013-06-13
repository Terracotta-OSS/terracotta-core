/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class GetAllCustomMap<K, V> extends AbstractMap<K, V> {
  private final Collection<K>      keys;
  private final Map<K, V>[]        internalMaps;
  private final AggregateServerMap clusteredMapImpl;
  private final boolean[]          fetchCompleted;
  private final boolean            quiet;
  private final int                getAllBatchSize;

  public GetAllCustomMap(Collection<K> keys, AggregateServerMap mapImpl, boolean quiet, int getAllBatchSize) {
    this.keys = Collections.unmodifiableCollection(keys);
    this.internalMaps = new Map[(int) Math.ceil((double) keys.size() / getAllBatchSize)];
    this.fetchCompleted = new boolean[internalMaps.length];
    this.clusteredMapImpl = mapImpl;
    this.quiet = quiet;
    this.getAllBatchSize = getAllBatchSize;
    initMaps();
    fetchValuesForIndex(0);
  }

  private void initMaps() {
    for (int index = 0; index < internalMaps.length; ++index) {
      internalMaps[index] = new HashMap<K, V>();
    }
    int index = 0;
    int counter = 0;
    // initialize all internal maps with <key,null>
    // TODO: distribute these keys based on which CDSMDso they resides in, internalMaps array size would become
    // concurrency but fetching one internalMap would be fast because all keys belong to one CDSMDso
    for (K key : keys) {
      if (counter == getAllBatchSize) {
        ++index;
        counter = 0;
      }
      internalMaps[index].put(key, null);
      ++counter;
    }
  }

  @Override
  public boolean containsKey(Object key) {
    return keys.contains(key);
  }

  @Override
  public boolean containsValue(Object value) {
    for (int i = 0; i < internalMaps.length; i++) {
      fetchValuesForIndex(i);
      if (internalMaps[i].containsValue(value)) return true;
    }
    return false;
  }

  @Override
  public V get(Object key) {
    int index = getMapIndexForKey(key);
    if (index == -1) { return null; }
    if (fetchCompleted[index]) { return internalMaps[index].get(key); }
    fetchValuesForIndex(index);
    return internalMaps[index].get(key);
  }

  private void fetchValuesForIndex(int index) {
    if (!fetchCompleted[index]) {
      synchronized (internalMaps[index]) {
        if (!fetchCompleted[index]) {
          internalMaps[index] = clusteredMapImpl.getAllInternal(internalMaps[index].keySet(), quiet);
          fetchCompleted[index] = true;
        }
      }
    }
  }

  private int getMapIndexForKey(Object key) {
    for (int index = 0; index < internalMaps.length; ++index) {
      if (internalMaps[index].containsKey(key)) { return index; }
    }
    return -1;
  }

  @Override
  public Set<K> keySet() {
    Set<K> keySet = new HashSet<K>();
    for (Map<K, V> internalMap : internalMaps) {
      keySet.addAll(internalMap.keySet());
    }
    return keySet;
  }

  @Override
  public Collection<V> values() {
    Collection<V> values = new ArrayList<V>(keys.size());
    for (int i = 0; i < internalMaps.length; i++) {
      fetchValuesForIndex(i);
      values.addAll(internalMaps[i].values());
    }
    return Collections.unmodifiableCollection(values);
  }
  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {

    @Override
    public Iterator<java.util.Map.Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    @Override
    public int size() {
      return keys.size();
    }
  }

  private final class EntryIterator implements Iterator<Entry<K, V>> {
    Iterator<Entry<K, V>> currentIterator;
    int                   index   = 0;
    Map<K, V>[]           intMaps = GetAllCustomMap.this.internalMaps;
    boolean[]             fetched = GetAllCustomMap.this.fetchCompleted;

    EntryIterator() {
      advance();
    }

    @Override
    public boolean hasNext() {
      return currentIterator.hasNext();
    }

    @Override
    public Map.Entry<K, V> next() {
      Map.Entry<K, V> entry = currentIterator.next();
      advance();
      return entry;
    }

    // this method essentially advance iterator from internal maps if needed
    private final void advance() {
      if (currentIterator == null) {
        currentIterator = intMaps[index].entrySet().iterator();
      } else {
        if (!currentIterator.hasNext()) {
          // we want advance to next iterator only when this iterator is exhausted
          if (index < intMaps.length - 1) {
            // we can advance to next iterator only if currentIterator is not the last iterator
            ++index;
            if (!fetched[index]) {
              GetAllCustomMap.this.fetchValuesForIndex(index);
            }
            currentIterator = intMaps[index].entrySet().iterator();
          } else {
            // we can not advance to next iterator because this iterator is the last iterator
          }
        } else {
          // we do not want to advance to next iterator because this iterator is not fully exhausted
        }
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove not supported");
    }
  }

}
