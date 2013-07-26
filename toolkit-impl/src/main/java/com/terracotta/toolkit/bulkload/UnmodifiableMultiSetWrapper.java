/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.bulkload;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class UnmodifiableMultiSetWrapper<T> extends AbstractSet<T> {

  private final List<Set<T>> delegates;

  public UnmodifiableMultiSetWrapper(Set<T>... delegates) {
    this.delegates = Collections.unmodifiableList(Arrays.asList(delegates));
  }

  @Override
  public boolean contains(Object obj) {
    for (Collection<? extends T> c : delegates) {
      if (c.contains(obj)) { return true; }
    }
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> coll) {
    for (Collection<? extends T> c : delegates) {
      if (c.containsAll(coll)) { return true; }
    }
    for (Object o : coll) {
      if (!contains(o)) { return false; }
    }
    return true;
  }

  @Override
  public boolean isEmpty() {
    for (Collection<?> c : delegates) {
      if (!c.isEmpty()) { return false; }
    }
    return true;
  }

  @Override
  public Iterator<T> iterator() {
    Collection<Iterator<T>> iterators = new ArrayList<Iterator<T>>(delegates.size());
    for (Collection<T> c : delegates) {
      iterators.add(c.iterator());
    }
    return new AggregateIterator<T>(iterators);
  }

  @Override
  public int size() {
    long totalSize = 0;
    for (Collection<?> c : delegates) {
      totalSize += c.size();
    }
    if (totalSize > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) totalSize;
    }
  }
}