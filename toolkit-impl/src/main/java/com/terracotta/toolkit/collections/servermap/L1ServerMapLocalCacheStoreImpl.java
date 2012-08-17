/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap;

import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.LocalCacheStoreFullException;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFullException;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class L1ServerMapLocalCacheStoreImpl<K, V> implements L1ServerMapLocalCacheStore<K, V> {
  private final List<L1ServerMapLocalCacheStoreListener<K, V>> listeners = new CopyOnWriteArrayList<L1ServerMapLocalCacheStoreListener<K, V>>();
  protected final ServerMapLocalStore<K, V>                    toolkitStore;

  public L1ServerMapLocalCacheStoreImpl(ServerMapLocalStore<K, V> toolkitStore) {
    this.toolkitStore = toolkitStore;
    toolkitStore.addListener(new L1ListenerAdapter());
  }

  public boolean addListener(L1ServerMapLocalCacheStoreListener<K, V> listener) {
    return listeners.add(listener);
  }

  public boolean removeListener(L1ServerMapLocalCacheStoreListener<K, V> listener) {
    return listeners.remove(listener);
  }

  public void clear() {
    toolkitStore.clear();
  }

  public void unpinAll() {
    toolkitStore.unpinAll();
  }

  public boolean isPinned(K key) {
    return toolkitStore.isPinned(key);
  }

  public void setPinned(K key, boolean pinned) {
    toolkitStore.setPinned(key, pinned);
  }

  public List getKeys() {
    return toolkitStore.getKeys();
  }

  public int size() {
    return toolkitStore.getSize() / 2;
  }

  private class L1ListenerAdapter implements ServerMapLocalStoreListener<K, V> {

    public void notifyElementEvicted(K key, V value) {
      for (L1ServerMapLocalCacheStoreListener<K, V> l : listeners) {
        l.notifyElementEvicted(key, value);
      }
    }

    public void notifyElementsEvicted(Map<K, V> evictedElements) {
      for (L1ServerMapLocalCacheStoreListener<K, V> l : listeners) {
        l.notifyElementsEvicted(evictedElements);
      }
    }

    public void notifyElementExpired(K key, V value) {
      for (L1ServerMapLocalCacheStoreListener<K, V> l : listeners) {
        l.notifyElementExpired(key, value);
      }
    }
  }

  public int offHeapSize() {
    return toolkitStore.getOffHeapSize() / 2;
  }

  public int onHeapSize() {
    return toolkitStore.getOnHeapSize() / 2;
  }

  public long offHeapSizeInBytes() {
    return toolkitStore.getOffHeapSizeInBytes();
  }

  public long onHeapSizeInBytes() {
    return toolkitStore.getOnHeapSizeInBytes();
  }

  public void dispose() {
    toolkitStore.dispose();
    for (L1ServerMapLocalCacheStoreListener<K, V> l : listeners) {
      l.notifyDisposed(this);
    }
  }

  public boolean containsKeyOffHeap(K key) {
    return toolkitStore.containsKeyOffHeap(key);
  }

  public boolean containsKeyOnHeap(K key) {
    return toolkitStore.containsKeyOnHeap(key);
  }

  public V put(K key, V value) throws LocalCacheStoreFullException {
    try {
      return toolkitStore.put(key, value);
    } catch (ServerMapLocalStoreFullException e) {
      throw new LocalCacheStoreFullException(e);
    }
  }

  public V get(K key) {
    return toolkitStore.get(key);
  }

  public V remove(K key) {
    return toolkitStore.remove(key);
  }

  public Object remove(K key, V value) {
    return toolkitStore.remove(key, value);
  }

  public int getMaxElementsInMemory() {
    return toolkitStore.getMaxEntriesLocalHeap() == 0 ? 0 : (toolkitStore.getMaxEntriesLocalHeap() - 1) / 2;
  }

  public void setMaxEntriesLocalHeap(int maxEntriesLocalHeap) {
    toolkitStore.setMaxEntriesLocalHeap(maxEntriesLocalHeap * 2 + 1);
  }

  public void setMaxBytesLocalHeap(long maxBytesLocalHeap) {
    toolkitStore.setMaxBytesLocalHeap(maxBytesLocalHeap);
  }

  public void recalculateSize(K key) {
    toolkitStore.recalculateSize(key);
  }
}
