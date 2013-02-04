/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap.api;

import java.util.List;

public interface ServerMapLocalStore<K, V> {

  public V put(K key, V value) throws ServerMapLocalStoreFullException;

  public V get(K key);

  public V remove(K key);

  public V remove(K key, V value);

  public void clear();

  public void cleanLocalState();

  public boolean addListener(ServerMapLocalStoreListener<K, V> listener);

  public boolean removeListener(ServerMapLocalStoreListener<K, V> listener);

  public List<K> getKeys();

  public int getMaxEntriesLocalHeap();

  public void setMaxEntriesLocalHeap(int newMaxEntriesLocalHeap);

  public void setMaxBytesLocalHeap(long newMaxBytesLocalHeap);

  public long getMaxBytesLocalHeap();

  public int getOffHeapSize();

  public int getOnHeapSize();

  public int getSize();

  public long getOnHeapSizeInBytes();

  public long getOffHeapSizeInBytes();

  public void dispose();

  public boolean containsKeyOnHeap(K key);

  public boolean containsKeyOffHeap(K key);

  public void recalculateSize(K key);

  public boolean isPinned();
}
