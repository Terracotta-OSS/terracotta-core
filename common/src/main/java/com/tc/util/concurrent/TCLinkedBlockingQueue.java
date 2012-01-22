/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TCLinkedBlockingQueue implements TCQueue {
  private final LinkedBlockingQueue queue;

  public TCLinkedBlockingQueue() {
    queue = new LinkedBlockingQueue();
  }

  public TCLinkedBlockingQueue(int capacity) {
    queue = new LinkedBlockingQueue(capacity);
  }

  public boolean offer(Object obj, long timeout) throws InterruptedException {
    return queue.offer(obj, timeout, TimeUnit.MILLISECONDS);
  }

  public Object peek() {
    return queue.peek();
  }

  public Object poll(long timeout) throws InterruptedException {
    return queue.poll(timeout, TimeUnit.MILLISECONDS);
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
