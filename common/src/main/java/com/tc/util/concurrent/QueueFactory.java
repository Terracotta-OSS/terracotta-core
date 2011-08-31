/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import java.lang.reflect.Constructor;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueFactory {
  public static final String BOUNDED_LINKED_QUEUE  = BoundedLinkedQueue.class.getName();
  public static final String LINKED_BLOCKING_QUEUE = LinkedBlockingQueue.class.getName();
  private boolean            useBoundedLinkedQueue = false;

  /**
   * The Queues will be created on the basis of the jvm 1.4 : TCBoundedLinkedQueue 1.5+ : TCLinkedBlockingQueue
   */
  public QueueFactory() {
    if (!Vm.isJDK15Compliant()) this.useBoundedLinkedQueue = true;
  }

  /**
   * The type of Queues to be created is under user's control Arg: whether u want BoundedLinkedQueue or
   * LinkedBlockingQueue true: BoundedLinkedQueue false: LinkedBlockingQueue
   */
  public QueueFactory(String className) {
    Assert.eval(className.equals(BOUNDED_LINKED_QUEUE) || className.equals(LINKED_BLOCKING_QUEUE));
    if (className.equals(BOUNDED_LINKED_QUEUE)) this.useBoundedLinkedQueue = true;
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

  /**
   * @return a TCQueue backed by either LinkedBlockingQueue or BoundedLinkedQueue with a capacity as the input
   *         parameter.
   * @throws IllegalArgumentException if the capacity is less than or equal to zero
   */
  public TCQueue createInstance(int capacity) {
    TCQueue queue = null;
    if (useBoundedLinkedQueue) {
      queue = new TCBoundedLinkedQueue(capacity);
    } else {
      queue = createTCLinkedBlockingQueue(capacity);
    }
    return queue;
  }

  private TCQueue createTCLinkedBlockingQueue(int capacity) {
    try {
      Class clazz = Class.forName("com.tc.util.concurrent.TCLinkedBlockingQueue");
      Class argsClass[] = new Class[1];
      argsClass[0] = int.class;
      Constructor constructor = clazz.getConstructor(argsClass);
      Object[] args = new Object[1];
      args[0] = Integer.valueOf(capacity);
      return (TCQueue) constructor.newInstance(args);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
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
