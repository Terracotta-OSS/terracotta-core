package com.tc.gbapi;

import java.util.Collection;
import java.util.Set;

public interface GBCache<K, V> {
  // TODO: what exception to catch when
  V put(K key, V value);

  V get(K key);

  V remove(K key);

  void removeAll(Collection<K> keys);

  boolean containsKey(K key);

  void addEvictionListener(GBCacheEvictionListener<K, V> listener);

  /**
   * Required when nulling byte array.<br>
   * Cant do this since this needs to be done after the value is already in cache.
   * 
   * @param key
   */
  void recalculateSize(K key);

  // Required for Ehcache
  /**
   * Support for Ehcache.isElementOnHeap()<br>
   */
  boolean containsKeyOnHeap(K k);

  /**
   * Support for Ehcache.isElementOffHeap()<br>
   */
  boolean containsKeyOffHeap(K k);

  /**
   * Support for non stop<br>
   * Support for Ehcache.getKeys()<br>
   */
  Set<K> keySet();

  /**
   * Support for local size
   * 
   * @return
   */
  long size();
}
