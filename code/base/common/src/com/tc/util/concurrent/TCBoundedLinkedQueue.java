/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

public class TCBoundedLinkedQueue implements FastQueue {
  private BoundedLinkedQueue queue;

  public TCBoundedLinkedQueue() {
    System.out.println("Bounded Linked Queue, JDK 1.4");
    queue = new BoundedLinkedQueue();
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
  
  public void setCapacity(int capacity){
    queue.setCapacity(capacity);
  }

}
