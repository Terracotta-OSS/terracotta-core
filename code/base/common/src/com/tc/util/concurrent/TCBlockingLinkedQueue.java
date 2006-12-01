/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import java.util.NoSuchElementException;

/**
 * An implementation of the TC style blocking queue interface. Uses a <code>LinkedQueue</code> instance from Doug
 * Lea's util.concurrent package to do the heavy lfting
 * 
 * @author teck
 */
public class TCBlockingLinkedQueue implements BlockingQueue {

  private static final long NO_WAIT = 0;
  private final LinkedQueue queue;

  /**
   * Factory method for creating instances of this class
   * 
   * @return a new blocking queue instance
   */
  public TCBlockingLinkedQueue() {
    queue = new LinkedQueue();
  }

  public boolean offer(Object o, long timeout) throws InterruptedException {
    if (null == o) { throw new NullPointerException("Cannot add null item to queue"); }

    return queue.offer(o, timeout);
  }

  public Object poll(long timeout) throws InterruptedException {
    return queue.poll(timeout);
  }

  public Object take() throws InterruptedException {
    return queue.take();
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }

  public Object element() {
    throw new UnsupportedOperationException();
  }

  public boolean offer(Object o) {
    try {
      return queue.offer(o, 0);
    } catch (InterruptedException e) {
      return false;
    }
  }

  public Object peek() {
    return queue.peek();
  }

  public Object poll() {
    try {
      return queue.poll(NO_WAIT);
    } catch (InterruptedException e) {
      return null;
    }
  }

  public Object remove() {
    Object rv = poll();

    if (rv == null) { throw new NoSuchElementException(); }

    return rv;
  }

}