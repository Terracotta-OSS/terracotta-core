/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.util;

import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TCMap;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public class ConcurrentHashMapKeySetWrapper extends AbstractSet {

  protected final Set realKeySet;
  protected final Map map;
  
  public ConcurrentHashMapKeySetWrapper(Map map, Set realKeySet) {
    this.realKeySet = realKeySet;
    this.map = map;
  }
  
  public boolean add(Object o) {
    return realKeySet.add(o);
  }

  public boolean addAll(Collection c) {
    return realKeySet.addAll(c);
  }

  public void clear() {
    realKeySet.clear();
  }

  public boolean contains(Object o) {
    return realKeySet.contains(o);
  }

  public boolean containsAll(Collection c) {
    return realKeySet.containsAll(c);
  }

  public boolean isEmpty() {
    return realKeySet.isEmpty();
  }

  public Iterator iterator() {
    if (((Manageable)map).__tc_isManaged()) {
      return new IteratorWrapper(this, realKeySet.iterator());
    } else {
      return realKeySet.iterator();
    }
  }

  public boolean remove(Object o) {
    if (((Manageable)map).__tc_isManaged()) {
      int sizeB4 = size();
      ((TCMap)map).__tc_remove_logical(o);
      return (size() != sizeB4);
    } else {
      return realKeySet.remove(o);
    }
  }

  public boolean removeAll(Collection c) {
    if (((Manageable)map).__tc_isManaged()) {
      boolean modified = false;

      if (size() > c.size()) {
        for (Iterator i = c.iterator(); i.hasNext();) {
          if (remove(i.next())) {
            modified = true;
          }
        }
      } else {
        for (Iterator i = iterator(); i.hasNext();) {
          if (c.contains(i.next())) {
            i.remove();
            modified = true;
          }
        }
      }
      
      return modified;
    } else {
      return realKeySet.removeAll(c);
    }
  }

  public boolean retainAll(Collection c) {
    if (((Manageable)map).__tc_isManaged()) {
      boolean modified = false;
      
      for (Iterator i = iterator(); i.hasNext();) {
        if (!c.contains(i.next())) {
          modified = true;
          i.remove();
        }
      }
      
      return modified;
    } else {
      return realKeySet.retainAll(c);
    }
  }

  public int size() {
    return realKeySet.size();
  }

  public Object[] toArray() {
    return realKeySet.toArray();
  }

  public Object[] toArray(Object[] a) {
    return realKeySet.toArray(a);
  }
  
  private static class IteratorWrapper implements Iterator {

    protected final Iterator realIterator;
    protected final Set keys;

    private Object latestKey;
    
    IteratorWrapper(Set keys, Iterator realIterator) {
      this.keys = keys;
      this.realIterator = realIterator;
    }

    public void remove() {
      if (latestKey == null)
        throw new IllegalStateException();
      
      keys.remove(latestKey);
      latestKey = null;
    }

    public final boolean hasNext() {
      return realIterator.hasNext();
    }

    public final Object next() {
      latestKey = realIterator.next();
      return latestKey;
    }
  }  
}
