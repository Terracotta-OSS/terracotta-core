/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.object.bytecode.ManagerUtil;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class THashMapCollectionWrapper implements Set {

  private final Collection realValues;
  private final Map        map;

  public THashMapCollectionWrapper(Map map, Collection realValues) {
    this.map = map;
    this.realValues = realValues;
  }

  public boolean add(Object o) {
    return realValues.add(o);
  }

  public boolean addAll(Collection c) {
    return realValues.addAll(c);
  }

  public void clear() {
    realValues.clear();
  }

  public boolean contains(Object o) {
    return realValues.contains(o);
  }

  public boolean containsAll(Collection c) {
    return realValues.containsAll(c);
  }

  public boolean equals(Object o) {
    return realValues.equals(o);
  }

  public int hashCode() {
    return realValues.hashCode();
  }

  public boolean isEmpty() {
    return realValues.isEmpty();
  }

  public Iterator iterator() {
    return new IteratorWrapper(map, realValues.iterator());
  }

  public boolean remove(Object o) {
    ManagerUtil.checkWriteAccess(map);
    return realValues.remove(o);
  }

  public boolean removeAll(Collection c) {
    return realValues.removeAll(c);
  }

  public boolean retainAll(Collection c) {
    return realValues.retainAll(c);
  }

  public int size() {
    return realValues.size();
  }

  public Object[] toArray() {
    return realValues.toArray();
  }

  public Object[] toArray(Object[] a) {
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
    private final Iterator realIterator;
    private final Map      map;
    private Object         lastValue;

    public IteratorWrapper(Map map, Iterator realIterator) {
      this.map = map;
      this.realIterator = realIterator;
    }

    public boolean hasNext() {
      return realIterator.hasNext();
    }

    public Object next() {
      lastValue = realIterator.next();
      return lastValue;
    }

    public void remove() {
      ManagerUtil.checkWriteAccess(map);
      realIterator.remove();
    }
  }

}