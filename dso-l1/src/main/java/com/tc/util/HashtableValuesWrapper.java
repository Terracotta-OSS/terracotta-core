/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.object.SerializationUtil;
import com.tc.object.TCObjectExternal;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

public class HashtableValuesWrapper implements Collection {

  private final Collection realValues;
  private final Hashtable  hashtable;

  public HashtableValuesWrapper(Hashtable hashtable, Collection realValues) {
    this.hashtable = hashtable;
    this.realValues = realValues;
  }

  public final boolean add(Object o) {
    return realValues.add(o);
  }

  public final boolean addAll(Collection c) {
    return realValues.addAll(c);
  }

  public final void clear() {
    realValues.clear();
  }

  public final boolean contains(Object o) {
    return realValues.contains(o);
  }

  public final boolean containsAll(Collection c) {
    return realValues.containsAll(c);
  }

  @Override
  public final boolean equals(Object o) {
    return realValues.equals(o);
  }

  @Override
  public final int hashCode() {
    return realValues.hashCode();
  }

  public final boolean isEmpty() {
    return realValues.isEmpty();
  }

  public final Iterator iterator() {
    return new IteratorWrapper(hashtable, realValues.iterator());
  }

  public final boolean remove(Object o) {
    Iterator iter = iterator();
    if (o == null) {
      while (iter.hasNext()) {
        if (iter.next() == null) {
          iter.remove();
          return true;
        }
      }
    } else {
      while (iter.hasNext()) {
        if (o.equals(iter.next())) {
          iter.remove();
          return true;
        }
      }
    }
    return false;
  }

  public final boolean removeAll(Collection c) {
    boolean modified = false;
    Iterator iter = iterator();
    while (iter.hasNext()) {
      if (c.contains(iter.next())) {
        iter.remove();
        modified = true;
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
    return realValues.size();
  }

  public final Object[] toArray() {
    return realValues.toArray();
  }

  public final Object[] toArray(Object[] a) {
    return realValues.toArray(a);
  }

  private static class IteratorWrapper implements Iterator {

    private final Iterator  realIterator;
    private final Hashtable hashtable;
    private Object          lastValue;

    public IteratorWrapper(Hashtable hashtable, Iterator realIterator) {
      this.hashtable = hashtable;
      this.realIterator = realIterator;
    }

    public final boolean hasNext() {
      return realIterator.hasNext();
    }

    public final Object next() {
      lastValue = realIterator.next();
      return lastValue;
    }

    public final void remove() {
      // XXX: This linear scan of the hashtable to find the proper key is bad. The only way to it (I think) would be to
      // instrument the actual hastable.values.iterator.remove code that has access to the key object right then and
      // there
      ManagerUtil.monitorEnter(hashtable, Manager.LOCK_TYPE_WRITE);

      try {
        TCObjectExternal tco = ManagerUtil.lookupExistingOrNull(hashtable);
        Object key = null;
        if (tco != null) {
          // find the key object iff this is a managed hashtable
          for (Iterator i = hashtable.entrySet().iterator(); i.hasNext();) {
            Entry entry = (Entry) i.next();
            if (entry.getValue().equals(lastValue)) {
              key = entry.getKey();
              break;
            }
          }

          if (key == null) { throw new AssertionError("Did not find key for value " + lastValue); }
        }

        realIterator.remove();

        // the real itearator can throw an exception. Do DSO work iff no exception thrown
        if (tco != null) {
          ManagerUtil.logicalInvoke(hashtable, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { key });
        }
      } finally {
        ManagerUtil.monitorExit(hashtable, Manager.LOCK_TYPE_WRITE);
      }
    }
  }

}