/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap.api.gbimpl;

import com.tc.exception.ImplementMe;
import com.tc.gbapi.GBCache;
import com.tc.gbapi.GBManager;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFullException;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreListener;

import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GBServerMapLocalStore<K, V> implements ServerMapLocalStore<K, V> {
  private final GBCache<K, V>                        gbCache;
  private final ReadWriteLock                        lock    = new ReentrantReadWriteLock();
  private final GBCacheEvictionListenerAdapter<K, V> adapter = new GBCacheEvictionListenerAdapter<K, V>();
  private final GBManager                            gbManager;
  private final String                               name;

  public GBServerMapLocalStore(String name, GBCache<K, V> gbCache, GBManager gbManager) {
    this.name = name;
    this.gbCache = gbCache;
    this.gbManager = gbManager;
    this.gbCache.addEvictionListener(adapter);
  }

  @Override
  public V put(K key, V value) throws ServerMapLocalStoreFullException {
    lock.writeLock().lock();

    try {
      return gbCache.put(key, value);
    } catch (Throwable t) {
      // TODO: what exception to catch here when PUT fails
      throw new ServerMapLocalStoreFullException(t);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public V get(K key) {
    lock.readLock().lock();

    try {
      return gbCache.get(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V remove(K key) {
    lock.writeLock().lock();
    try {
      return gbCache.remove(key);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public V remove(K key, V value) {
    lock.writeLock().lock();
    try {
      V objectFetched = gbCache.get(key);
      if (objectFetched == null || !value.equals(objectFetched)) { return null; }
      V removed = gbCache.remove(key);
      if (removed != null) { return removed; }
      return null;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void clear() {
    // TODO: dispose can handle this
    throw new UnsupportedOperationException();
  }

  @Override
  public void unpinAll() {
    // TODO: create another pinned map
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isPinned(K key) {
    // TODO: create another pinned map
    throw new UnsupportedOperationException();
  }

  @Override
  public void setPinned(K key, boolean pinned) {
    // TODO: create another pinned map
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addListener(ServerMapLocalStoreListener<K, V> listener) {
    return adapter.addListener(listener);
  }

  @Override
  public boolean removeListener(ServerMapLocalStoreListener<K, V> listener) {
    return adapter.removeListener(listener);
  }

  @Override
  public List<K> getKeys() {
    lock.readLock().lock();

    try {
      return asList(gbCache.keySet());
    } finally {
      lock.readLock().unlock();
    }
  }

  private List<K> asList(Set<K> keySet) {
    // TODO: List was used because of ehcache, can be easily changed to Set
    return null;
  }

  @Override
  public int getMaxEntriesLocalHeap() {
    // TODO: store config locally or ask GBCache ?
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMaxEntriesLocalHeap(int newMaxEntriesLocalHeap) {
    // TODO: how to support
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMaxBytesLocalHeap(long newMaxBytesLocalHeap) {
    throw new ImplementMe();

  }

  @Override
  public long getMaxBytesLocalHeap() {
    // TODO: store config locally or ask GBCache ?
    throw new UnsupportedOperationException();
  }

  @Override
  public int getOffHeapSize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getOnHeapSize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getSize() {
    lock.readLock().lock();

    try {
      return (int) gbCache.size();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public long getOnHeapSizeInBytes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getOffHeapSizeInBytes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dispose() {
    // TODO:
    gbManager.detachMap(name);
  }

  @Override
  public boolean containsKeyOnHeap(K key) {
    lock.readLock().lock();

    try {
      return gbCache.containsKeyOnHeap(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean containsKeyOffHeap(K key) {
    lock.readLock().lock();

    try {
      return gbCache.containsKeyOffHeap(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void recalculateSize(K key) {
    lock.writeLock().lock();

    try {
      gbCache.recalculateSize(key);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean isLocalHeapOrMemoryTierPinned() {
    // TODO: create another pinned map
    throw new UnsupportedOperationException();
  }
}
