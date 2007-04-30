/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class LinkedHashMap implements Cloneable, Map, Serializable {

  private Map map = null;
  private LinkedList list = null;
  private boolean accessOrder = false;

  public LinkedHashMap() {
    map = new HashMap();
    list = new LinkedList();
  }
  
  public LinkedHashMap(int initialCapacity) {
    map = new HashMap(initialCapacity);
    list = new LinkedList();
  }
  
  public LinkedHashMap(int initialCapacity, float loadFactor) {
    map = new HashMap(initialCapacity, loadFactor);
    list = new LinkedList();
  }

  public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
    map = new HashMap(initialCapacity, loadFactor);
    list = new LinkedList();
    this.accessOrder = accessOrder;    
  }
  
  public LinkedHashMap(Map m) {
    map = new HashMap(m);
    list = new LinkedList(map.entrySet());
  }

  public void clear() {
    synchronized (this) {
      map.clear();
      list.clear();
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
    return map.isEmpty() && list.isEmpty();
  }

  public Set keySet() {
    System.err.println("this: " + System.identityHashCode(this));
    return map.keySet();
  }

  public Object put(Object key, Object value) {
    Object o = map.put(key, value);
    list.add(o);
    return o;
  }

  public void putAll(Map t) {
    map.putAll(t);
    list.addAll(map.entrySet());
  }

  public Object remove(Object key) {
    Object o = map.remove(key);
    list.remove(o);
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
