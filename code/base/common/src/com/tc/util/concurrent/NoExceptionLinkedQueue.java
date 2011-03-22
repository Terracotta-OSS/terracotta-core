/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.util.Util;

public class NoExceptionLinkedQueue implements Channel {
  public final LinkedQueue queue = new LinkedQueue();

  public void put(Object o) {
    boolean interrupted = false;
    while (true) {
      try {
        queue.put(o);
        Util.selfInterruptIfNeeded(interrupted);
        return;
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
  }

  public boolean offer(Object o, long l) {
    try {
      return queue.offer(o, l);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }

  public Object peek() {
    return queue.peek();
  }

  public Object poll(long arg0) {
    try {
      return queue.poll(arg0);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  public Object take() {
    boolean interrupted = false;
    while (true) {
      try {
        Object o = queue.take();
        Util.selfInterruptIfNeeded(interrupted);
        return o;
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NoExceptionLinkedQueue) {
      return queue.equals(((NoExceptionLinkedQueue) obj).queue);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return queue.hashCode();
  }

  @Override
  public String toString() {
    return queue.toString();
  }
}
