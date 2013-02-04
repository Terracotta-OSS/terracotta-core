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
import java.util.concurrent.CopyOnWriteArrayList;

public class L1ServerMapLocalCacheStoreImpl<K, V> implements L1ServerMapLocalCacheStore<K, V> {
  private final List<L1ServerMapLocalCacheStoreListener<K, V>> listeners = new CopyOnWriteArrayList<L1ServerMapLocalCacheStoreListener<K, V>>();
  protected final ServerMapLocalStore<K, V>                    toolkitStore;

  public L1ServerMapLocalCacheStoreImpl(ServerMapLocalStore<K, V> toolkitStore) {
    this.toolkitStore = toolkitStore;
    toolkitStore.addListener(new L1ListenerAdapter());
  }

  @Override
  public boolean addListener(L1ServerMapLocalCacheStoreListener<K, V> listener) {
    return listeners.add(listener);
  }

  @Override
  public boolean removeListener(L1ServerMapLocalCacheStoreListener<K, V> listener) {
    return listeners.remove(listener);
  }

  @Override
  public void clear() {
    toolkitStore.clear();
  }

  @Override
  public void cleanLocalState() {
    toolkitStore.cleanLocalState();
  }

  @Override
  public List getKeys() {
    return toolkitStore.getKeys();
  }

  @Override
  public int size() {
    return toolkitStore.getSize() / 2;
  }

  private class L1ListenerAdapter implements ServerMapLocalStoreListener<K, V> {

    @Override
    public void notifyElementEvicted(K key, V value) {
      for (L1ServerMapLocalCacheStoreListener<K, V> l : listeners) {
        l.notifyElementEvicted(key, value);
      }
    }
  }

  @Override
  public int offHeapSize() {
    return toolkitStore.getOffHeapSize() / 2;
  }

  @Override
  public int onHeapSize() {
    return toolkitStore.getOnHeapSize() / 2;
  }

  @Override
  public long offHeapSizeInBytes() {
    return toolkitStore.getOffHeapSizeInBytes();
  }

  @Override
  public long onHeapSizeInBytes() {
    return toolkitStore.getOnHeapSizeInBytes();
  }

  @Override
  public void dispose() {
    toolkitStore.dispose();
    for (L1ServerMapLocalCacheStoreListener<K, V> l : listeners) {
      l.notifyDisposed(this);
    }
  }

  @Override
  public boolean containsKeyOffHeap(K key) {
    return toolkitStore.containsKeyOffHeap(key);
  }

  @Override
  public boolean containsKeyOnHeap(K key) {
    return toolkitStore.containsKeyOnHeap(key);
  }

  @Override
  public V put(K key, V value) throws LocalCacheStoreFullException {
    try {
      return toolkitStore.put(key, value);
    } catch (ServerMapLocalStoreFullException e) {
      throw new LocalCacheStoreFullException(e);
    }
  }

  @Override
  public V get(K key) {
    return toolkitStore.get(key);
  }

  @Override
  public V remove(K key) {
    return toolkitStore.remove(key);
  }

  @Override
  public Object remove(K key, V value) {
    return toolkitStore.remove(key, value);
  }

  @Override
  public int getMaxElementsInMemory() {
    return toolkitStore.getMaxEntriesLocalHeap() == 0 ? 0 : (toolkitStore.getMaxEntriesLocalHeap() - 1) / 2;
  }

  @Override
  public void setMaxEntriesLocalHeap(int maxEntriesLocalHeap) {
    toolkitStore.setMaxEntriesLocalHeap(maxEntriesLocalHeap * 2 + 1);
  }

  @Override
  public void setMaxBytesLocalHeap(long maxBytesLocalHeap) {
    toolkitStore.setMaxBytesLocalHeap(maxBytesLocalHeap);
  }

  @Override
  public void recalculateSize(K key) {
    toolkitStore.recalculateSize(key);
  }

  @Override
  public boolean isPinned() {
    return toolkitStore.isPinned();
  }
}
