/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

/**
 * Adds methods to the regular queue interface for blocking queue implementations. Similar to <code>Queue</code>,
 * this interface is modelled after the java.util.concurrent.BlockingQueue interface
 * 
 * @author orion
 */
public interface BlockingQueue extends Queue {
  /**
   * Place item in queue only if it can be accepted within the given timeout
   * 
   * @param o the object to offer to the queue (should be non-null)
   * @param timeout the number of milliseconds to wait. If less than or equal to zero, do not perform any timed waits
   * @return true if accepted, else false
   * @throws InterruptedException if the current thread has been interrupted at a point at which interruption is
   *         detected, in which case the element is guaranteed not to be inserted (i.e., is equivalent to a false
   *         return).
   */
  boolean offer(Object o, long timeout) throws InterruptedException;

  /**
   * Return and remove an item from channel only if one is available within the given timeout period
   * 
   * @param timeout the number of milliseconds to wait. If less than or equal to zero, do not perform any timed waits
   * @return an item, or null if the channel is empty.
   * @throws InterruptedException if the current thread has been interrupted at a point at which interruption is
   *         detected, in which case state of the channel is unchanged (i.e., equivalent to a null return).
   */
  Object poll(long timeout) throws InterruptedException;

  /**
   * Return and remove an item from this queue, possibly waiting indefinitely until such an item exists.
   * 
   * @return an item from the queue. Order of the return item (vs. insertion order) is governed by the implementation
   * @throws InterruptedException if the current thread has been interrupted at a point at which interruption is
   *         detected, in which case state of the queue is unchanged.
   */
  Object take() throws InterruptedException;
}