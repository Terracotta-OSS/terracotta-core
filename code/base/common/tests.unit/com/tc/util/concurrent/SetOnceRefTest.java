/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

import junit.framework.TestCase;

/**
 * test cases for SetOnceRef
 * 
 * @author teck
 */
public class SetOnceRefTest extends TestCase {

  public void testAllowsNull() {
    SetOnceRef ref = new SetOnceRef(false);

    assertFalse(ref.allowsNull());

    try {
      ref.set(null);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }

    Object val = new Object();
    ref.set(val);

    assertTrue(ref.get() == val);
  }

  public void testSetNull() {
    final SetOnceRef ref1 = new SetOnceRef(true);
    ref1.set(null);
    final SetOnceRef ref2 = new SetOnceRef(null, true);

    assertTrue(ref1.get() == null);
    assertTrue(ref2.get() == null);
  }

  public void testSetTwice() {
    final Object val1 = new Object();
    final Object val2 = new Object();

    final SetOnceRef ref1 = new SetOnceRef();
    ref1.set(val1);
    final SetOnceRef ref2 = new SetOnceRef(val2);

    assertTrue(ref1.get() == val1);
    assertTrue(ref2.get() == val2);

    try {
      ref1.set(val1);
      fail();
    } catch (IllegalStateException e) {
      // expected
    }

    try {
      ref2.set(val2);
      fail();
    } catch (IllegalStateException e) {
      // expected
    }
  }

  public void testGetMany() {
    final Object JUnitGetsMeHard = new Object();
    final SetOnceRef ref = new SetOnceRef(JUnitGetsMeHard);

    for (int i = 0; i < 1000; i++) {
      assertTrue(ref.get() == JUnitGetsMeHard);
    }
  }

  public void testThreadAccess() throws InterruptedException {
    final Object val = new Object();

    final SetOnceRef ref = new SetOnceRef();

    Thread other = new Thread(new Runnable() {
      public void run() {
        ref.set(val);
      }
    });

    other.start();
    other.join();

    assertTrue(ref.get() == val);
  }

}
