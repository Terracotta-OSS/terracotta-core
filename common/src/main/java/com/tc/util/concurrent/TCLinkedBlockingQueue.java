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

  @Override
  public boolean offer(Object obj, long timeout) throws InterruptedException {
    return queue.offer(obj, timeout, TimeUnit.MILLISECONDS);
  }

  @Override
  public Object peek() {
    return queue.peek();
  }

  @Override
  public Object poll(long timeout) throws InterruptedException {
    return queue.poll(timeout, TimeUnit.MILLISECONDS);
  }

  @Override
  public void put(Object obj) throws InterruptedException {
    queue.put(obj);
  }

  @Override
  public Object take() throws InterruptedException {
    return queue.take();
  }

  @Override
  public int size() {
    return queue.size();
  }

  @Override
  public boolean isEmpty() {
    return queue.isEmpty();
  }

}
