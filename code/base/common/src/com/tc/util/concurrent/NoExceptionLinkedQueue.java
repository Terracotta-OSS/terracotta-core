/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.util.Util;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

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

  public boolean equals(Object obj) {
    return queue.equals(obj);
  }

  public int hashCode() {
    return queue.hashCode();
  }

  public String toString() {
    return queue.toString();
  }
}
