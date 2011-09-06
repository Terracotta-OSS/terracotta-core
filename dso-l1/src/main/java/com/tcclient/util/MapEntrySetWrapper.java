/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.util;

import com.tc.object.SerializationUtil;
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
public class MapEntrySetWrapper implements Set {
  public final static String CLASS_SLASH = "com/tcclient/util/MapEntrySetWrapper";

  protected final Set realEntrySet;
  protected final Map map;

  public MapEntrySetWrapper(Map map, Set realEntrySet) {
    this.realEntrySet = realEntrySet;
    this.map = map;
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
    ManagerUtil.checkWriteAccess(map);
    boolean removed = realEntrySet.remove(o);
    if (removed) {
      ManagerUtil.logicalInvoke(map, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { ((Map.Entry) o).getKey() });
    }
    return removed;
  }

  public final boolean removeAll(Collection c) {
    boolean modified = false;

    if (size() > c.size()) {
      for (Iterator i = c.iterator(); i.hasNext();)
        modified |= remove(i.next());
    } else {
      for (Iterator i = iterator(); i.hasNext();) {
        if (c.contains(i.next())) {
          i.remove();
          modified = true;
        }
      }
    }
    return modified;
  }

  public final boolean retainAll(Collection c) {
    boolean modified = false;
    Iterator i = iterator();
    while (i.hasNext()) {
      if (!c.contains(i.next())) {
        i.remove();
        modified = true;
      }
    }
    return modified;

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
      ManagerUtil.objectArrayChanged(a, size, null);
    }
    return a;
  }

  private static class IteratorWrapper implements Iterator {

    protected final Iterator realIterator;
    protected final Map      map;
    protected Entry          current;

    IteratorWrapper(Map map, Iterator realIterator) {
      this.map = map;
      this.realIterator = realIterator;
    }

    public void remove() {
      ManagerUtil.checkWriteAccess(map);
      realIterator.remove();

      // important to do this after the real remove() since an exception can be thrown (never
      // started, at end, concurrent mod, etc)
      ManagerUtil.logicalInvoke(map, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { current.getKey() });
    }

    public final boolean hasNext() {
      boolean rv = realIterator.hasNext();
      return rv;
    }

    public final Object next() {
      current = new EntryWrapper(map, (Entry) realIterator.next());
      return current;
    }

  }

  protected static class EntryWrapper implements Entry {
    private final Object key;
    private Object       value;
    private final Map    map;

    EntryWrapper(Map map, Entry entry) {
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
