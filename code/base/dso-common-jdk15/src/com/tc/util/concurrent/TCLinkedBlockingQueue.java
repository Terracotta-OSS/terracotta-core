/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TCLinkedBlockingQueue implements TCQueue {
  private LinkedBlockingQueue queue = null;

  public TCLinkedBlockingQueue(){
    queue = new LinkedBlockingQueue();
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

  public void put(Object obj) {
    queue.add(obj);
  }

  public Object take() {
    return queue.remove();
  }

  public int size() {
    return queue.size();
  }

  public void setCapacity(int capacity) {
    //do nothing
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }

}
