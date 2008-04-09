/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import java.lang.reflect.Constructor;

public class QueueFactory {
  public static final String BoundedLinkedQueue    = BoundedLinkedQueue.class.getName();
  public static final String LinkedBlockingQueue    =  "java.util.concurrent.LinkedBlockingQueue";
  private boolean            useBoundedLinkedQueue = false;

  /**
   * The Queues will be created on the basis of the jvm 1.4 : TCBoundedLinkedQueue 1.5+ : TCLinkedBlockingQueue
   */
  public QueueFactory() {
    if (!Vm.isJDK15Compliant()) this.useBoundedLinkedQueue = true;
  }

  /**
   * The type of Queues to be created is under user's control 
   * Arg: whether u want BoundedLinkedQueue or LinkedBlockingQueue 
   * true: BoundedLinkedQueue 
   * false: LinkedBlockingQueue
   */
  public QueueFactory(String className) {
    Assert.eval(className.equals(BoundedLinkedQueue) || className.equals(LinkedBlockingQueue));
    if (className.equals(BoundedLinkedQueue)) this.useBoundedLinkedQueue = true;
    else this.useBoundedLinkedQueue = false;
  }

  public TCQueue createInstance() {
    TCQueue queue = null;
    if (useBoundedLinkedQueue) {
      queue = new TCBoundedLinkedQueue();
    } else {
      queue = createTCLinkedBlockingQueue();
    }
    return queue;
  }

  public TCQueue createInstance(int capacity) {
    TCQueue queue = null;
    if (useBoundedLinkedQueue) {
      queue = new TCBoundedLinkedQueue(capacity);
    } else {
      queue = createTCLinkedBlockingQueue();
      queue.setCapacity(capacity);
    }
    return queue;
  }

  private TCQueue createTCLinkedBlockingQueue() {
    try {
      Class clazz = Class.forName("com.tc.util.concurrent.TCLinkedBlockingQueue");
      Constructor constructor = clazz.getConstructor(new Class[0]);
      return (TCQueue) constructor.newInstance(new Object[0]);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
