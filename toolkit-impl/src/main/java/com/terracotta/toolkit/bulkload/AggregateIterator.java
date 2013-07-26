/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.bulkload;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * AggregateIterator is a iterator. It is use to iterate over set of iterator. Example BulkLoadCache iterator should
 * aggregate with LocalBufferMap Iterator.
 */
public class AggregateIterator<T> implements Iterator<T> {

  protected final Iterator<Iterator<T>> iterators;
  protected Iterator<T>                 currentIterator;

  private Iterator<T> getNextIterator() {
    return iterators.next();
  }

  public AggregateIterator(Collection<Iterator<T>> iterators) {
    this.iterators = iterators.iterator();
    while (this.iterators.hasNext()) {
      this.currentIterator = getNextIterator();
      if (this.currentIterator.hasNext()) { return; }
    }
  }

  @Override
  public boolean hasNext() {

    if (this.currentIterator == null) { return false; }
    boolean hasNext = false;

    if (this.currentIterator.hasNext()) {
      hasNext = true;
    } else {
      while (this.iterators.hasNext()) {
        this.currentIterator = getNextIterator();
        if (this.currentIterator.hasNext()) { return true; }
      }
    }

    return hasNext;
  }

  @Override
  public T next() {

    if (this.currentIterator == null) { throw new NoSuchElementException(); }

    if (this.currentIterator.hasNext()) {
      return this.currentIterator.next();

    } else {
      while (this.iterators.hasNext()) {
        this.currentIterator = getNextIterator();

        if (this.currentIterator.hasNext()) { return this.currentIterator.next(); }
      }
    }

    throw new NoSuchElementException();
  }

  @Override
  public void remove() {
    this.currentIterator.remove();
  }

}
