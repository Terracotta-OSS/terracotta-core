/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class LocalStoreKeySet extends AbstractSet<Object> {

  public static interface LocalStoreKeySetFilter {
    boolean accept(Object value);
  }

  private final int                    size;
  private final Set                    internalSet;
  private final LocalStoreKeySetFilter filter;

  public LocalStoreKeySet(Set internalSet, int size, LocalStoreKeySetFilter filter) {
    this.internalSet = internalSet;
    this.size = size;
    this.filter = filter;
  }

  @Override
  public Iterator<Object> iterator() {
    return new FilteringIterator(internalSet.iterator(), filter);
  }

  @Override
  public int size() {
    return size;
  }

  private static class FilteringIterator implements Iterator {

    private final Iterator               internalIterator;
    private final LocalStoreKeySetFilter filter;
    private Object                       currentNext;
    private boolean                      nextAvailable;

    public FilteringIterator(Iterator internalIterator, LocalStoreKeySetFilter filter) {
      this.internalIterator = internalIterator;
      this.filter = filter;
    }

    public synchronized boolean hasNext() {
      if (nextAvailable) { return true; }
      while (internalIterator.hasNext()) {
        Object object = internalIterator.next();
        if (filter.accept(object)) {
          currentNext = object;
          nextAvailable = true;
          return true;
        }
      }
      return false;
    }

    public synchronized Object next() {
      if (hasNext()) {
        Object rv = currentNext;
        currentNext = null;
        nextAvailable = false;
        return rv;
      }
      throw new NoSuchElementException();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

}
