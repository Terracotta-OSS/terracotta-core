package com.tc.gbapi;

import java.util.Set;

/**
 * @author Alex Snaps
 */
public interface GBTwoKeyCache<K1, K2, V> extends Iterable<GBTwoKeyCache.Entry<K1, K2, V>> {

  /**
   *
   * @param probe
   * @param key
   * @param value
   * @return
   * @throws IllegalArgumentException on violation of the unique constraint of second key
   */
  Entry<K1, K2, V> putUsingKey1(K1 probe, K2 key, V value) throws IllegalArgumentException;

  /**
   * @throws IllegalArgumentException on violation of the unique constraint of first key
   */
  Entry<K1, K2, V> putUsingKey2(K1 key, K2 probe, V value) throws IllegalArgumentException;

  Entry<K1, K2, V> removeUsingKey1(K1 key);

  Entry<K1, K2, V> removeUsingKey2(K2 key);

  boolean remove(K1 probe1, K2 probe2, V value);

  @Deprecated // Because this could be done using the iterator maybe... Ask Abhishek S
  Set<K1> getKeys1();

  @Deprecated // Because this could be done using the iterator maybe... Ask Abhishek S
  Set<K2> getKeys2();

  V getUsingKey1(K1 key);

  V getUsingKey2(K2 key);

  V get(K1 key1, K2 key2);

  public interface Entry<K1, K2, V> {
    K1 getKey1();
    K2 getKey2();
    V getValue();
  }

  public interface EvictionListener<K1, K2, V> {
    void notifyEviction(Entry<K1, K2, V> entry);
  }
}
