/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.PutType;
import com.tc.object.servermap.localcache.RemoveType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class L1ServerMapLocalCacheStoreHashMap<K, V> implements L1ServerMapLocalCacheStore<K, V> {
  private final List<L1ServerMapLocalCacheStoreListener<K, V>> listeners     = new CopyOnWriteArrayList<L1ServerMapLocalCacheStoreListener<K, V>>();
  private final HashMap<K, V>                                  backingCache  = new HashMap<K, V>();
  private final HashSet<K>                                     pinnedEntries = new HashSet<K>();
  private final AtomicInteger                                  cacheSize     = new AtomicInteger();
  private final int                                            maxElementsInMemory;

  public L1ServerMapLocalCacheStoreHashMap() {
    this(0);
  }

  public L1ServerMapLocalCacheStoreHashMap(int maxElementsInMemory) {
    this.maxElementsInMemory = maxElementsInMemory;
  }

  public boolean addListener(L1ServerMapLocalCacheStoreListener<K, V> listener) {
    return listeners.add(listener);
  }

  public synchronized V get(K key) {
    return backingCache.get(key);
  }

  public V put(K key, V value, PutType putType) {
    V oldValue = null;
    synchronized (this) {
      oldValue = backingCache.put(key, value);
      if (putType.isPinned()) {
        pinEntry(key);
      }
    }

    if (oldValue == null && putType.incrementSizeOnPut()) {
      cacheSize.incrementAndGet();
    }

    return oldValue;
  }

  private synchronized void pinEntry(K key) {
    pinnedEntries.add(key);
  }

  public synchronized void unpinEntry(K key) {
    pinnedEntries.remove(key);
  }

  public V remove(K key, RemoveType removeType) {
    final V value;
    synchronized (this) {
      value = backingCache.remove(key);
      pinnedEntries.remove(key);
    }
    if (removeType.decrementSizeOnRemove()) {
      cacheSize.decrementAndGet();
    }
    return value;
  }

  public boolean removeListener(L1ServerMapLocalCacheStoreListener<K, V> listener) {
    return listeners.remove(listener);
  }

  private void notifyListeners(Map<K, V> evictedElements) {
    for (L1ServerMapLocalCacheStoreListener<K, V> listener : listeners) {
      listener.notifyElementsEvicted(evictedElements);
    }
  }

  public int evict(int count) {
    Map<K, V> tempMap = new HashMap<K, V>();
    synchronized (this) {
      int deletedElements = 0;
      Iterator<Entry<K, V>> iterator = backingCache.entrySet().iterator();
      while (iterator.hasNext() && deletedElements < count) {
        Entry<K, V> entry = iterator.next();
        if (pinnedEntries.contains(entry.getKey())) {
          continue;
        }
        tempMap.put(entry.getKey(), entry.getValue());
        iterator.remove();
        cacheSize.decrementAndGet();
        deletedElements++;
      }
    }

    notifyListeners(tempMap);
    return tempMap.size();
  }

  public synchronized int size() {
    return this.cacheSize.get();
  }

  // TODO: Remove it using an iterator
  public synchronized void clear() {
    backingCache.clear();
    cacheSize.set(0);
  }

  public synchronized Set getKeySet() {
    return new HashSet(this.backingCache.keySet());
  }

  public int getMaxElementsInMemory() {
    return maxElementsInMemory;
  }

  @Override
  public String toString() {
    return "L1ServerMapLocalCacheStoreHashMap [backingCache=" + backingCache.size() + " " + backingCache
           + "\npinnedEntries=" + pinnedEntries.size() + " " + pinnedEntries + "]";
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

  public void shutdown() {
    // Nothing to do
  }
}
