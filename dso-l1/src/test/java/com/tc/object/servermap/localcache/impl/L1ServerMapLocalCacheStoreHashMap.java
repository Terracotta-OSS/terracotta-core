/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.servermap.localcache.L1ServerMapLocalCacheLockProvider;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.PutType;
import com.tc.object.servermap.localcache.RemoveType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class L1ServerMapLocalCacheStoreHashMap<K, V> implements L1ServerMapLocalCacheStore<K, V> {
  private final List<L1ServerMapLocalCacheStoreListener<K, V>> listeners     = new CopyOnWriteArrayList<L1ServerMapLocalCacheStoreListener<K, V>>();
  private final HashMap<K, V>                                  backingCache  = new HashMap<K, V>();
  private final HashSet<K>                                     pinnedEntries = new HashSet<K>();
  private final AtomicInteger                                  cacheSize     = new AtomicInteger();
  private final int                                            maxElementsInMemory;
  private final L1ServerMapLocalCacheLockProvider              lockProvider  = new DummyLockProvider();

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
    checkIfEvictionIsRequired();

    return oldValue;
  }

  private void checkIfEvictionIsRequired() {
    if (maxElementsInMemory == 0) { return; }

    Map evictedEntries = new HashMap();

    synchronized (this) {
      if (cacheSize.get() <= maxElementsInMemory) { return; }
      int overshoot = cacheSize.get() - maxElementsInMemory;
      int evicted = 0;
      for (Iterator iter = backingCache.entrySet().iterator(); iter.hasNext() && evicted <= overshoot;) {
        Entry entry = (Entry) iter.next();
        if (pinnedEntries.contains(entry.getKey())) {
          continue;
        }
        evictedEntries.put(entry.getKey(), entry.getValue());
        iter.remove();
        cacheSize.decrementAndGet();
        evicted++;
      }
    }

    if (evictedEntries.size() > 0) {
      notifyListeners(evictedEntries);
    }
  }

  private synchronized void pinEntry(K key) {
    pinnedEntries.add(key);
  }

  public synchronized void unpinEntry(K key, V value) {
    pinnedEntries.remove(key);
  }

  public V remove(K key, RemoveType removeType) {
    final V value;
    synchronized (this) {
      value = backingCache.remove(key);
      pinnedEntries.remove(key);
    }
    if (value != null && removeType.decrementSizeOnRemove()) {
      cacheSize.decrementAndGet();
    }
    return value;
  }

  public Object remove(K key, V value, RemoveType removeType) {
    return remove(key, removeType);
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

  public int onHeapSize() {
    return cacheSize.get();
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

  public L1ServerMapLocalCacheLockProvider getLockProvider() {
    return lockProvider;
  }

  private static class DummyLockProvider implements L1ServerMapLocalCacheLockProvider {

    private final int                               numLocks;
    private final ArrayList<ReentrantReadWriteLock> locks;

    public DummyLockProvider() {
      numLocks = 256;
      locks = new ArrayList<ReentrantReadWriteLock>(numLocks);
      initilizeLocks();
    }

    private void initilizeLocks() {
      for (int i = 0; i < numLocks; ++i) {
        locks.add(new ReentrantReadWriteLock());
      }
    }

    public ReentrantReadWriteLock getLock(Object key) {
      return locks.get(Math.abs(key.hashCode()) % numLocks);
    }

    public Collection<ReentrantReadWriteLock> getAllLocks() {
      return locks;
    }
  }

}
