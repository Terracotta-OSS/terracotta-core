/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.TCRuntimeException;

public class NoExceptionLinkedQueue implements Channel {
  public final LinkedQueue queue = new LinkedQueue();
  
  public void put(Object o) {
    try {
      queue.put(o);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }
  
  public boolean offer(Object o, long l) {
    try {
      return queue.offer(o, l);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
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
      throw new TCRuntimeException(e);
    }
  }

  public Object take() {
    try {
      return queue.take();
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
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
