/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import junit.framework.TestCase;

public class WaitInvocationTest extends TestCase {

  public void testCstrs() throws Exception {
    TimerSpec wait0 = new TimerSpec();
    assertEquals(TimerSpec.NO_ARGS, wait0.getSignature());
    
    TimerSpec wait1 = new TimerSpec(1);    
    assertEquals(TimerSpec.LONG, wait1.getSignature());
    assertEquals(1, wait1.getMillis());
    
    TimerSpec wait2 = new TimerSpec(2, 3);
    assertEquals(TimerSpec.LONG_INT, wait2.getSignature());
    assertEquals(2, wait2.getMillis());
    assertEquals(3, wait2.getNanos());

    new TimerSpec(0);
    new TimerSpec(1);
    new TimerSpec(Long.MAX_VALUE);

    try {
      new TimerSpec(-1);
      fail("no exception thrown");
    } catch (IllegalArgumentException iae) {
      // expected
    }

    new TimerSpec(0, 0);
    new TimerSpec(1, 0);
    new TimerSpec(Long.MAX_VALUE, 0);
    new TimerSpec(Long.MAX_VALUE, Integer.MAX_VALUE);

    try {
      new TimerSpec(-1, 0);
      fail("no exception thrown");
    } catch (IllegalArgumentException iae) {
      // expected
    }

    try {
      new TimerSpec(-1, -1);
      fail("no exception thrown");
    } catch (IllegalArgumentException iae) {
      // expected
    }

    try {
      new TimerSpec(0, -1);
      fail("no exception thrown");
    } catch (IllegalArgumentException iae) {
      // expected
    }
  }

}
