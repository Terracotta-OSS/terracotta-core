/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import com.tc.abortable.AbortableOperationManagerImpl;

import junit.framework.Assert;
import junit.framework.TestCase;

public class NonStopManagerTest extends TestCase {
  private AbortableOperationManagerImpl abortableOperationManager;
  private NonStopManagerImpl            nonStopManager;

  @Override
  protected void setUp() throws Exception {
    abortableOperationManager = new AbortableOperationManagerImpl();
    nonStopManager = new NonStopManagerImpl(abortableOperationManager);
  }

  public void testNonStopTimer() throws Exception {
    long startTime = System.currentTimeMillis();
    int loopTmes = 50;
    long timeout = 500;
    for (int i = 0; i < loopTmes; i++) {
      System.out.println("executing loop count" + i);
      nonStopManager.begin(timeout);
      try {
        blockUntilAborted();
      } finally {
        Assert.assertTrue(abortableOperationManager.isAborted());
        Thread.currentThread().interrupt();
        nonStopManager.finish();
        // check that aborted status is cleared.
        Assert.assertFalse(abortableOperationManager.isAborted());
        // check that interrupted flag is cleared.
        Assert.assertFalse(Thread.interrupted());
      }
    }
    long timeTaken = System.currentTimeMillis() - startTime;
    System.out.println("time taken to execute operations " + timeTaken);
    Assert.assertTrue((timeTaken > loopTmes * timeout) && timeTaken < (loopTmes * timeout + 2000));
  }

  public void testTryBegin() throws Throwable {
    Assert.assertTrue(nonStopManager.tryBegin(10000));
    Assert.assertFalse(nonStopManager.tryBegin(10000));
    nonStopManager.finish();

    nonStopManager.begin(10000);
    Assert.assertFalse(nonStopManager.tryBegin(10000));
    nonStopManager.finish();

    Assert.assertTrue(nonStopManager.tryBegin(10000));
    try {
      nonStopManager.begin(-1);
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected
    } finally {
      nonStopManager.finish();
    }
  }

  public void testBoundaryTimeouts() {
    // test min value.
    nonStopManager.begin(Long.MIN_VALUE);
    nonStopManager.finish();
    // test negative value
    nonStopManager.begin(-1);
    nonStopManager.finish();
    // test zero
    nonStopManager.begin(0);
    nonStopManager.finish();
    // test max value
    nonStopManager.begin(Long.MAX_VALUE);
    nonStopManager.finish();
  }

  private void blockUntilAborted() {
    while (true) {
      try {
        Thread.currentThread().join();
      } catch (InterruptedException e) {
        if (abortableOperationManager.isAborted()) { return; }
      }
    }
  }

  public void testIllegalStates() throws Throwable {
    // finish without begin
    try {
      nonStopManager.finish();
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected
    }
    // multiple begin
    nonStopManager.begin(10000);
    try {
      nonStopManager.begin(1000);
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected
    } finally {
      nonStopManager.finish();
    }

    // multiple finish
    nonStopManager.begin(10000);
    nonStopManager.finish();
    try {
      nonStopManager.finish();
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected
    }
  }
}
