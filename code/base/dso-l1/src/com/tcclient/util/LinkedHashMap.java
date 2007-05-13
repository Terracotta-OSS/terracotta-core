/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class LinkedHashMap implements Cloneable, Map, Serializable {

  private static final int DEFAULT_CAPACITY = 16;
  private static final float DEFAULT_LOAD_FACTOR = 0.75f;
  private static final boolean DEFAULT_ACCESS_ORDER = false;
  private Map map = null;

  public LinkedHashMap() {
	this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_ACCESS_ORDER);
  }
  
  public LinkedHashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_ACCESS_ORDER);
  }
  
  public LinkedHashMap(int initialCapacity, float loadFactor) {
    this(initialCapacity, loadFactor, DEFAULT_ACCESS_ORDER);
  }

  public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
    map = new java.util.LinkedHashMap(initialCapacity, loadFactor, accessOrder);
  }
  
  public LinkedHashMap(Map m) {
    map = new java.util.LinkedHashMap(m);
  }

  public void clear() {
    synchronized (this) {
      map.clear();
    }
  }

  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  public Set entrySet() {
    return map.keySet();
  }

  public Object get(Object key) {
    return map.get(key);
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public Set keySet() {
    return map.keySet();
  }

  public Object put(Object key, Object value) {
    Object o = map.put(key, value);
    return o;
  }

  public void putAll(Map t) {
    map.putAll(t);
  }

  public Object remove(Object key) {
    Object o = map.remove(key);
    return o;
  }

  public int size() {
    return map.size();
  }

  public Collection values() {
    return map.values();
  }

  protected boolean removeEldestEntry(Map.Entry eldest) {
    return false;
  }
}
