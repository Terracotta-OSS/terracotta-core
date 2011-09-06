/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import java.util.Map;

/**
 * This interface would be called when an eviction happens in the L1ServerMapLocalStore.<br>
 * Currently this interface should be called only when:<br>
 * 1) capacity eviction happens<br>
 * 2) evict (count) gets called on L1ServerMapLocalStore<br>
 */
public interface L1ServerMapLocalCacheStoreListener<K, V> {

  /**
   * When a key gets evicted.
   */
  public void notifyElementEvicted(K key, V value);

  /**
   * When a set if keys get evicted.
   */
  public void notifyElementsEvicted(Map<K, V> evictedElements);

  /**
   * When a key is expired
   */
  public void notifyElementExpired(K key, V value);

  /**
   * Called whenever a store is diposed of
   */
  public void notifyDisposed(L1ServerMapLocalCacheStore store);
}
