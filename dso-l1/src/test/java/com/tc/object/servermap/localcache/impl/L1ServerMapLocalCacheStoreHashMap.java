/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class L1ServerMapLocalCacheStoreHashMap<K, V> implements L1ServerMapLocalCacheStore<K, V> {
  private final List<L1ServerMapLocalCacheStoreListener<K, V>> listeners    = new CopyOnWriteArrayList<L1ServerMapLocalCacheStoreListener<K, V>>();
  private final HashMap<K, V>                                  backingCache = new HashMap<K, V>();
  private final int                                            maxElementsInMemory;

  public L1ServerMapLocalCacheStoreHashMap() {
    this(0);
  }

  public L1ServerMapLocalCacheStoreHashMap(int maxElementsInMemory) {
    this.maxElementsInMemory = maxElementsInMemory * 2;
  }

  public boolean addListener(L1ServerMapLocalCacheStoreListener<K, V> listener) {
    return listeners.add(listener);
  }

  public synchronized V get(K key) {
    return backingCache.get(key);
  }

  public V put(K key, V value) {
    V oldValue = null;
    synchronized (this) {
      oldValue = backingCache.put(key, value);
    }
    checkIfEvictionIsRequired();

    return oldValue;
  }

  private void checkIfEvictionIsRequired() {
    if (maxElementsInMemory == 0) { return; }

    Map evictedEntries = new HashMap();

    synchronized (this) {
      if (backingCache.size() <= maxElementsInMemory) { return; }
      int overshoot = backingCache.size() - maxElementsInMemory;
      int evicted = 0;
      for (Iterator iter = backingCache.entrySet().iterator(); iter.hasNext() && evicted <= overshoot;) {
        Entry entry = (Entry) iter.next();
        evictedEntries.put(entry.getKey(), entry.getValue());
        iter.remove();
        evicted++;
      }
    }

    if (evictedEntries.size() > 0) {
      notifyListeners(evictedEntries);
    }
  }

  public V remove(K key) {
    final V value;
    synchronized (this) {
      value = backingCache.remove(key);
    }
    return value;
  }

  public Object remove(K key, V value) {
    return remove(key);
  }

  public boolean removeListener(L1ServerMapLocalCacheStoreListener<K, V> listener) {
    return listeners.remove(listener);
  }

  private void notifyListeners(Map<K, V> evictedElements) {
    for (L1ServerMapLocalCacheStoreListener<K, V> listener : listeners) {
      listener.notifyElementsEvicted(evictedElements);
    }
  }

  public synchronized int size() {
    return backingCache.size() / 2;
  }

  // TODO: Remove it using an iterator
  public synchronized void clear() {
    backingCache.clear();
  }

  public synchronized Set getKeySet() {
    return new HashSet(this.backingCache.keySet());
  }

  public int getMaxElementsInMemory() {
    return maxElementsInMemory / 2;
  }

  @Override
  public String toString() {
    return "L1ServerMapLocalCacheStoreHashMap [backingCache=" + backingCache.size() + " " + backingCache;
  }

  public List<L1ServerMapLocalCacheStoreListener<K, V>> getListeners() {
    return listeners;
  }

  public long onHeapSizeInBytes() {
    // HashMap doesn't have heap size calculations.
    return 0;
  }

  public long offHeapSizeInBytes() {
    // No offheap for the simple hashmap case
    return 0;
  }

  public int onHeapSize() {
    return backingCache.size() / 2;
  }

  public int offHeapSize() {
    return 0;
  }

  public void dispose() {
    // Nothing to dispose of
  }

  public boolean containsKeyOnHeap(K key) {
    return backingCache.containsKey(key);
  }

  public boolean containsKeyOffHeap(K key) {
    return false;
  }

  public void setMaxEntriesLocalHeap(int maxEntriesLocalHeap) {
    // Not used for this local cache store implementation
  }

  public void setMaxBytesLocalHeap(long maxBytesLocalHeap) {
    // Not used for this local cache store implementation
  }

  public void replace(K key, V oldValue, V newValue) {
    synchronized (this) {
      if (backingCache.containsKey(key)) {
        backingCache.put(key, newValue);
      }
    }
  }

}
