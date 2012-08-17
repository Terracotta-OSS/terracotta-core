/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.object.Destroyable;

import java.util.Iterator;

class DestroyableIterator<E> implements Iterator<E> {

  private static final String ERROR_MSG = "The ToolkitObject associated with this iterator has already been destroyed";
  private final Iterator<E>   iterator;
  private final Destroyable   destroyable;

  public DestroyableIterator(Iterator<E> iterator, Destroyable destroyable) {
    this.iterator = iterator;
    this.destroyable = destroyable;
  }

  @Override
  public boolean hasNext() {
    if (!destroyable.isDestroyed()) { return iterator.hasNext(); }
    throw new IllegalStateException(ERROR_MSG);
  }

  @Override
  public E next() {
    if (!destroyable.isDestroyed()) { return iterator.next(); }
    throw new IllegalStateException(ERROR_MSG);
  }

  @Override
  public void remove() {
    if (!destroyable.isDestroyed()) {
      iterator.remove();
      return;
    }
    throw new IllegalStateException(ERROR_MSG);
  }

}
