package com.tc.gbapi.impl;

import com.tc.gbapi.GBMap;
import com.tc.gbapi.GBMapMutationListener;
import com.tc.gbapi.GBRetriever;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Alex Snaps
 */
public class GBOnHeapMapImpl<K, V> implements GBMap<K, V> {

  private final ConcurrentMap<K, V> store = new ConcurrentHashMap<K, V>();

  private final ReadWriteLock[] locks;
  private final int segmentShift;
  private final int segmentMask;
  private final List<GBMapMutationListener<K, V>> mutationListeners;

  public GBOnHeapMapImpl() {
    this(null);
  }

  public GBOnHeapMapImpl(final List<? extends GBMapMutationListener<K, V>> mutationListeners) {
    this(mutationListeners, 512);
  }

  public GBOnHeapMapImpl(final List<? extends GBMapMutationListener<K, V>> mutationListeners, final int concurrency) {
    int sshift = 0;
    int ssize = 1;
    while (ssize < concurrency) {
      ++sshift;
      ssize <<= 1;
    }
    segmentShift = 32 - sshift;
    segmentMask = ssize - 1;
    this.locks = new ReadWriteLock[concurrency];
    for (int i = 0, locksLength = locks.length; i < locksLength; i++) {
      locks[i] = new ReentrantReadWriteLock();
    }
    if (mutationListeners != null && !mutationListeners.isEmpty()) {
      this.mutationListeners = Collections.unmodifiableList(new ArrayList<GBMapMutationListener<K, V>>(mutationListeners));
    } else {
      this.mutationListeners = null;
    }
  }

  @Override
  public Set<K> keySet() {
    return store.keySet();
  }

  @Override
  public Collection<V> values() {
    return store.values();
  }

  @Override
  public long size() {
    return store.size();
  }

  @Override
  public void put(final K key, final V value) {
    final Lock lock = getLockFor(key.hashCode()).writeLock();
    lock.lock();
    try {
      store.put(key, value);
      notifyAdd(key, value);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public V get(final K key) {
    final Lock lock = getLockFor(key.hashCode()).readLock();
    lock.lock();
    try {
      return store.get(key);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean remove(final K key) {
    final Lock lock = getLockFor(key.hashCode()).writeLock();
    lock.lock();
    try {
      final V previous = store.remove(key);
      notifyRemove(key, previous);
      return previous != null;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void removeAll(final Collection<K> keys) {
    for (K key : keys) {
      remove(key);
    }
  }

  @Override
  public boolean containsKey(final K key) {
    final Lock lock = getLockFor(key.hashCode()).readLock();
    lock.lock();
    try {
      return store.containsKey(key);
    } finally {
      lock.unlock();
    }
  }

  private void notifyAdd(final K key, final V value) {
    if (mutationListeners != null) {
      for (GBMapMutationListener<K, V> mutationListener : mutationListeners) {
        mutationListener.added(new GBOnHeapRetriever<K>(key), new GBOnHeapRetriever<V>(value), null);
      }
    }
  }

  private void notifyRemove(final K key, final V value) {
    if (mutationListeners != null) {
      for (GBMapMutationListener<K, V> mutationListener : mutationListeners) {
        mutationListener.removed(new GBOnHeapRetriever<K>(key), new GBOnHeapRetriever<V>(value), null);
      }
    }
  }

  private ReadWriteLock getLockFor(int hash) {
    return locks[(spread(hash) >>> segmentShift) & segmentMask];
  }

  private static int spread(int hash) {
    int h = hash;
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  @Override
  public void clear() {
    store.clear();
  }
}

class GBOnHeapRetriever<T> implements GBRetriever<T> {

  private final T value;

  GBOnHeapRetriever(final T value) {
    this.value = value;
  }

  @Override
  public T retrieve() {
    return value;
  }
}
