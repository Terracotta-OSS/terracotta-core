/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import junit.framework.TestCase;

public class TCLinkedQueueJDK14Test extends TestCase {

  public void testLinkedQueue() {
    System.out.println(" --TEST CASE : testLinkedQueue");
    if (!Vm.isJDK14()) {
      System.out.println("This test is supposed to run only for JDK 1.4. Exiting the test...");
      return;
    }
    TCQueue queue = (new QueueFactory()).createInstance();
    Assert.assertTrue(queue instanceof TCBoundedLinkedQueue);
  }
  
  public void testLinkedQueueCapacity() {
    System.out.println(" --TEST CASE : testLinkedQueueCapacity");
    if (!Vm.isJDK14()) {
      System.out.println("This test is supposed to run only for JDK 1.4. Exiting the test...");
      return;
    }
    int capacity = 100;
    TCQueue linkedBlockingQueue = (new QueueFactory()).createInstance(capacity);
    Assert.assertTrue(linkedBlockingQueue instanceof TCBoundedLinkedQueue);

    for (int i = 0; i < capacity; i++) {
      try {
        linkedBlockingQueue.put(new Integer(i));
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    // Now try to offer, and it should fail
    try {
      boolean offered = linkedBlockingQueue.offer(new Integer(1000), 0);
      assertFalse(offered);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

    // try creating with negative capacity.
    try {
      linkedBlockingQueue = (new QueueFactory()).createInstance(-1);
      throw new AssertionError("Expected to throw an Exception");
    } catch (IllegalArgumentException iae) {
      // expected
    }
  }
}