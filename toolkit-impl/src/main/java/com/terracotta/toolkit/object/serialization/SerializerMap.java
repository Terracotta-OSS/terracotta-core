/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

public interface SerializerMap<K, V> {

  public V put(K key, V value);

  public V get(K key);

  public V localGet(K key);
}
