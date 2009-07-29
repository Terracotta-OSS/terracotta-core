/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/*
 * LazyMap creates underneath Map only when more than one entry added. Not support null key.
 */
public abstract class LazyMap<K, V> implements Map<K, V> {
  public static final int   MAP_SIZE    = 2;
  public static final float LOAD_FACTOR = 1F;

  private Map<K, V>         map         = null;
  private K                 key         = null;
  private V                 value       = null;

  abstract Map create();

  private boolean isSameKey(K aKey) {
    if (key == null) {
      return false;
    } else {
      return key.equals(aKey);
    }
  }

  private boolean isEntryExist() {
    return (key != null);
  }

  public V put(K aKey, V aValue) {
    if (aKey == null) throw new NullPointerException("LazyHash not support null key");

    if (map != null) { return map.put(aKey, aValue); }

    if (!isEntryExist()) {
      this.key = aKey;
      this.value = aValue;
      return null;
    } else if (isSameKey(aKey)) {
      // add the same key
      V v = value;
      value = aValue;
      return v;
    } else {
      // when add more than one entry, put all into map
      map = create();
      map.put(key, value);
      key = null;
      value = null;
      return map.put(aKey, aValue);
    }
  }

  public V get(Object aKey) {
    if (map != null) { return map.get(aKey); }

    if (isSameKey((K) aKey)) {
      return value;
    } else {
      return null;
    }
  }

  public void clear() {
    if (map != null) {
      map = null;
    }

    key = null;
    value = null;
  }

  public boolean containsKey(Object aKey) {
    if (map != null) { return map.containsKey(aKey); }

    return isSameKey((K) aKey);
  }

  public boolean containsValue(Object aValue) {
    if (map != null) { return map.containsValue(aValue); }

    if (key != null) {
      if (value == null) {
        return (aValue == null);
      } else {
        return value.equals(aValue);
      }
    }
    return false;
  }

  public Set<Map.Entry<K, V>> entrySet() {
    if (map != null) { return map.entrySet(); }

    Map m = create();
    if (isEntryExist()) {
      m.put(key, value);
    }
    return m.entrySet();
  }

  public boolean isEmpty() {
    if (map != null) { return map.isEmpty(); }

    return (!isEntryExist());
  }

  public Set<K> keySet() {
    if (map != null) { return map.keySet(); }

    Set<K> s = new HashSet<K>();
    if (isEntryExist()) {
      s.add(key);
    }
    return s;
  }

  public void putAll(Map<? extends K, ? extends V> m) {
    if (m.size() == 0) return;
    if (map == null) {
      map = create();
      if (isEntryExist()) {
        map.put(key, value);
        key = null;
        value = null;
      }
    }
    // same as map.putAll(m) but loop to abort null key
    for (Iterator<? extends Map.Entry<? extends K, ? extends V>> i = m.entrySet().iterator(); i.hasNext();) {
      Map.Entry<? extends K, ? extends V> e = i.next();
      if (e.getKey() == null) throw new NullPointerException("LazyHash not support null key");
      put(e.getKey(), e.getValue());
    }

  }

  public V remove(Object aKey) {
    if (map != null) { return map.remove(aKey); }

    if (isSameKey((K) aKey)) {
      V v = value;
      key = null;
      value = null;
      return v;
    } else {
      return null;
    }
  }

  public int size() {
    if (map != null) { return map.size(); }

    return (isEntryExist()) ? 1 : 0;
  }

  public Collection<V> values() {
    if (map != null) { return map.values(); }

    Collection<V> v = new HashSet<V>();
    if (isEntryExist()) {
      v.add(value);
    }
    return v;
  }

  public static class LazyHashMap<LK, LV> extends LazyMap<LK, LV> {
    Map<LK, LV> create() {
      return new HashMap<LK, LV>(MAP_SIZE, LOAD_FACTOR);
    }
  }

  public static class LazyLinkedHashMap<KK, VV> extends LazyMap<KK, VV> {
    Map<KK, VV> create() {
      return new LinkedHashMap<KK, VV>(MAP_SIZE, LOAD_FACTOR);
    }
  }

}
