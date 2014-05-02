package com.terracotta.toolkit.object.serialization;

import java.util.HashMap;
import java.util.Map;

/**
* @author tim
*/
class LocalSerializerMap<K, V> implements SerializerMap<K, V> {
  private final Map<K, V> localHashMap = new HashMap<K, V>();

  @Override
  public V put(K key, V value) {
    return localHashMap.put(key, value);
  }

  @Override
  public V get(K key) {
    return localHashMap.get(key);
  }

  @Override
  public V localGet(K key) {
    return get(key);
  }

}
