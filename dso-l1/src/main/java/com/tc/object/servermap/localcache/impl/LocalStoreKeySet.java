/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object.servermap.localcache.impl;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class LocalStoreKeySet extends AbstractSet<Object> {

  public static interface LocalStoreKeySetFilter {
    boolean accept(Object value);
  }

  private final int                    size;
  private final List                   internalSet1;
  private final Set                    internalSet2;
  private final LocalStoreKeySetFilter filter;

  public LocalStoreKeySet(List internalSet1, Set internalSet2, int size, LocalStoreKeySetFilter filter) {
    this.internalSet1 = internalSet1;
    this.internalSet2 = internalSet2;
    this.size = size;
    this.filter = filter;
  }

  @Override
  public Iterator<Object> iterator() {
    return new FilteringIterator(internalSet1.iterator(), internalSet2.iterator(), filter);
  }

  @Override
  public int size() {
    return size;
  }

  private static class FilteringIterator implements Iterator {

    private final Iterator               internalIterator1;
    private final Iterator               internalIterator2;
    private final LocalStoreKeySetFilter filter;
    private Object                       currentNext;
    private Iterator                     currentIterator;
    private boolean                      nextAvailable;

    public FilteringIterator(Iterator internalIterator1, Iterator internalIterator2, LocalStoreKeySetFilter filter) {
      this.internalIterator1 = internalIterator1;
      this.internalIterator2 = internalIterator2;
      this.currentIterator = this.internalIterator1;
      this.filter = filter;
    }

    @Override
    public synchronized boolean hasNext() {
      if (nextAvailable) { return true; }
      while (currentIterator.hasNext()) {
        Object object = currentIterator.next();
        if (filter.accept(object)) {
          currentNext = object;
          nextAvailable = true;
          return true;
        }
      }
      if (retry()) { return hasNext(); }
      return false;
    }

    private boolean retry() {
      if (currentIterator == internalIterator2) { return false; }
      currentIterator = internalIterator2;
      return true;
    }

    @Override
    public synchronized Object next() {
      if (hasNext()) {
        Object rv = currentNext;
        currentNext = null;
        nextAvailable = false;
        return rv;
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }
}
