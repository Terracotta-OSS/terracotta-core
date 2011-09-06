/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.util;

import com.tc.object.bytecode.ManagerUtil;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A wrapper for Map.entrySet() that keeps DSO informed of changes
 */
@SuppressWarnings("unchecked")
public class ConcurrentHashMapEntrySetWrapper implements Set {
  public final static String CLASS_SLASH = "com/tc/util/ConcurrentHashMapEntrySetWrapper";

  protected final Set realEntrySet;
  protected final Map map;

  public ConcurrentHashMapEntrySetWrapper(Map map, Set realEntrySet) {
    this.realEntrySet = realEntrySet;
    this.map = map;
  }
  
  public Set getDelegateEntrySet() {
    return realEntrySet;
  }

  public final boolean add(Object o) {
    return realEntrySet.add(o);
  }

  public final boolean addAll(Collection c) {
    return realEntrySet.addAll(c);
  }

  public final void clear() {
    realEntrySet.clear();
  }

  public final boolean contains(Object o) {
    return realEntrySet.contains(o);
  }

  public final boolean containsAll(Collection c) {
    return realEntrySet.containsAll(c);
  }

  public final boolean equals(Object o) {
    return realEntrySet.equals(o);
  }

  public final int hashCode() {
    return realEntrySet.hashCode();
  }

  public final boolean isEmpty() {
    return realEntrySet.isEmpty();
  }

  public Iterator iterator() {
    return new IteratorWrapper(map, realEntrySet.iterator());
  }

  public boolean remove(Object o) {
    return realEntrySet.remove(o);
  }

  public final boolean removeAll(Collection c) {
    return realEntrySet.removeAll(c);
  }

  public final boolean retainAll(Collection c) {
    return realEntrySet.retainAll(c);
  }

  public final int size() {
    return realEntrySet.size();
  }

  public final Object[] toArray() {
    return realEntrySet.toArray();
  }

  public final Object[] toArray(Object[] a) {
    int size = size();
    if (a.length < size) a = (Object[]) Array.newInstance(((Object) (a)).getClass().getComponentType(), size);

    int index = 0;
    for (Iterator iterator = iterator(); iterator.hasNext();) {
      ManagerUtil.objectArrayChanged(a, index++, iterator.next());
    }

    if (a.length > size) {
      a[size] = null;
    }
    return a;
  }

  private static class IteratorWrapper implements Iterator {

    protected final Iterator realIterator;
    protected final Map      map;

    IteratorWrapper(Map map, Iterator realIterator) {
      this.map = map;
      this.realIterator = realIterator;
    }

    public void remove() {
      realIterator.remove();
    }

    public final boolean hasNext() {
      boolean rv = realIterator.hasNext();
      return rv;
    }

    public final Object next() {
      return new EntryWrapper(map, (Entry) realIterator.next());
    }
  }

  public static class EntryWrapper implements Entry {
    private final Object key;
    private Object       value;
    private final Map    map;

    public EntryWrapper(Map map, Entry entry) {
      this.map = map;
      this.key = entry.getKey();
      this.value = entry.getValue();
    }

    public final boolean equals(Object o) {
      if (!(o instanceof Entry)) { return false; }
      Entry e2 = (Entry) o;
      return (key == null ? e2.getKey() == null : key.equals(e2.getKey()))
             && (value == null ? e2.getValue() == null : value.equals(e2.getValue()));
    }

    public final Object getKey() {
      return key;
    }

    public final Object getValue() {
      return value;
    }

    public final int hashCode() {
      return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }

    public final Object setValue(Object value) {
      Object rv = map.put(this.key, value);
      this.value = value;
      return rv;
    }

    public String toString() {
      return key + "=" + value;
    }
  }
}
