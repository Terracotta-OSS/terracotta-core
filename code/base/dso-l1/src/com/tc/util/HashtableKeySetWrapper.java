/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class HashtableKeySetWrapper implements Set {

  private final Set       realKeySet;
  private final Hashtable hashtable;

  public HashtableKeySetWrapper(Hashtable hashtable, Set realKeySet) {
    this.hashtable = hashtable;
    this.realKeySet = realKeySet;
  }

  public final boolean add(Object o) {
    // will throw an exception
    return realKeySet.add(o);
  }

  public final boolean addAll(Collection c) {
    // will throw an exception
    return realKeySet.addAll(c);
  }

  public final void clear() {
    // calls through to Hashtable.clear()
    realKeySet.clear();
  }

  public final boolean contains(Object o) {
    return realKeySet.contains(o);
  }

  public final boolean containsAll(Collection c) {
    return realKeySet.containsAll(c);
  }

  public final boolean equals(Object o) {
    return realKeySet.equals(o);
  }

  public final int hashCode() {
    return realKeySet.hashCode();
  }

  public final boolean isEmpty() {
    return realKeySet.isEmpty();
  }

  public final Iterator iterator() {
    return new IteratorWrapper(hashtable, realKeySet.iterator());
  }

  public final boolean remove(Object o) {
    // calls through to Hashtable.remove()
    return realKeySet.remove(o);
  }

  public final boolean removeAll(Collection c) {
    // ends up calling remove() on this, or remove() on this.iterator()
    return realKeySet.removeAll(c);
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
    return realKeySet.size();
  }

  public final Object[] toArray() {
    return realKeySet.toArray();
  }

  public final Object[] toArray(Object[] a) {
    return realKeySet.toArray(a);
  }

  public static class IteratorWrapper implements Iterator {

    private final Iterator  realIterator;
    private final Hashtable hashtable;
    private Object          last;

    public IteratorWrapper(Hashtable hashtable, Iterator realIterator) {
      this.hashtable = hashtable;
      this.realIterator = realIterator;
    }

    public final void remove() {
      ManagerUtil.monitorEnter(hashtable, Manager.LOCK_TYPE_WRITE);
      try {
        realIterator.remove();
        // Do the real remove first. If no exception thrown, then proceed with the DSO stuff
        ManagerUtil.logicalInvoke(hashtable, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { last });
      } finally {
        ManagerUtil.monitorExit(hashtable, Manager.LOCK_TYPE_WRITE);
      }
    }

    public final boolean hasNext() {
      return realIterator.hasNext();
    }

    public final Object next() {
      last = realIterator.next();
      return last;
    }
  }

}
