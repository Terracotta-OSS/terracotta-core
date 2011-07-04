/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

public class TCBoundedLinkedQueue implements TCQueue {
  private final BoundedLinkedQueue queue;

  public TCBoundedLinkedQueue() {
    queue = new BoundedLinkedQueue();
  }

  public TCBoundedLinkedQueue(int capacity) {
    queue = new BoundedLinkedQueue(capacity);
  }

  public boolean offer(Object obj, long timeout) throws InterruptedException {
    return queue.offer(obj, timeout);
  }

  public Object peek() {
    return queue.peek();
  }

  public Object poll(long timeout) throws InterruptedException {
    return queue.poll(timeout);
  }

  public void put(Object obj) throws InterruptedException {
    queue.put(obj);
  }

  public Object take() throws InterruptedException {
    return queue.take();
  }

  public int size() {
    return queue.size();
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }

}
