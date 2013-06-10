/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.util.ToolkitObjectStatus;

import java.util.Iterator;

public class StatusAwareIterator<E> implements Iterator<E> {

  private final Iterator<E>         iterator;
  private final ToolkitObjectStatus status;
  private final int                 currentRejoinCount;

  public StatusAwareIterator(Iterator<E> iterator, ToolkitObjectStatus status) {
    this.iterator = iterator;
    this.status = status;
    this.currentRejoinCount = this.status.getCurrentRejoinCount();
  }

  private void assertStatus() {
    if (status.isDestroyed()) { throw new IllegalStateException(
                                                                "Can not perform operation because object has been destroyed"); }
    if (this.currentRejoinCount != status.getCurrentRejoinCount()) { throw new RejoinException(
                                                                                               "Can not performe operation because rejoin happened."); }
  }

  @Override
  public boolean hasNext() {
    assertStatus();
    return iterator.hasNext();
  }

  @Override
  public E next() {
    assertStatus();
    return iterator.next();
  }

  @Override
  public void remove() {
    assertStatus();
    iterator.remove();
  }

}
