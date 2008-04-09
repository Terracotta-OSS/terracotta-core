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
}