/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

public class QueueFactory {

  /**
   * @return a TCQueue backed by LinkedBlockingQueue
   */
  public TCQueue createInstance() {
    return new TCLinkedBlockingQueue();
  }

  /**
   * @return a TCQueue backed by LinkedBlockingQueue with a capacity as the input parameter.
   * @throws IllegalArgumentException if the capacity is less than or equal to zero
   */
  public TCQueue createInstance(int capacity) {
    return new TCLinkedBlockingQueue(capacity);
  }

}
