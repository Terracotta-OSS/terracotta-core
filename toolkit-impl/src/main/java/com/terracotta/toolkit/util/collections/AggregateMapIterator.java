/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util.collections;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public abstract class AggregateMapIterator<T> implements Iterator<T> {

  protected final Iterator<? extends Map> listIterator;
  protected Iterator<T>                   currentIterator;

  public AggregateMapIterator(Iterator<? extends Map> listIterator) {
    this.listIterator = listIterator;
    while (this.listIterator.hasNext()) {
      this.currentIterator = getNextIterator();
      if (this.currentIterator.hasNext()) { return; }
    }
  }

  private Iterator<T> getNextIterator() {
    return getClusterMapIterator(listIterator.next());
  }

  public abstract Iterator<T> getClusterMapIterator(Map map);

  @Override
  public boolean hasNext() {

    if (this.currentIterator == null) { return false; }
    boolean hasNext = false;

    if (this.currentIterator.hasNext()) {
      hasNext = true;
    } else {
      while (this.listIterator.hasNext()) {
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
      while (this.listIterator.hasNext()) {
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
