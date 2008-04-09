/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.util.runtime.Vm;

import java.lang.reflect.Constructor;

public class QueueFactory {
  private boolean            useBoundedLinkedQueue = false;

  /**
   * The Queues will be created on the basis of the jvm
   * 1.4  : TCBoundedLinkedQueue
   * 1.5+ : TCLinkedBlockingQueue
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
  public QueueFactory(boolean useBoundedLinkedQueue) {
    this.useBoundedLinkedQueue = useBoundedLinkedQueue;
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
