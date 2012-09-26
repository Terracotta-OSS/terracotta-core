/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap.api.gbimpl;

import com.tc.gbapi.GBCacheEvictionListener;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreListener;

import java.util.concurrent.CopyOnWriteArraySet;

public class GBCacheEvictionListenerAdapter<K, V> implements GBCacheEvictionListener<K, V> {
  private final CopyOnWriteArraySet<ServerMapLocalStoreListener<K, V>> listeners = new CopyOnWriteArraySet<ServerMapLocalStoreListener<K, V>>();

  @Override
  public void notifyElementEvicted(K key, V value) {
    for (ServerMapLocalStoreListener<K, V> listener : listeners) {
      listener.notifyElementEvicted(key, value);
    }
  }
  
  public boolean addListener(ServerMapLocalStoreListener<K, V> listener) {
    return listeners.add(listener);
  }

  public boolean removeListener(ServerMapLocalStoreListener<K, V> listener) {
    return listeners.remove(listener);
  }

}
