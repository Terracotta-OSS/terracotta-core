/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import junit.framework.TestCase;

public class WaitInvocationTest extends TestCase {

  public void testCstrs() throws Exception {
    WaitInvocation wait0 = new WaitInvocation();
    assertEquals(WaitInvocation.NO_ARGS, wait0.getSignature());
    
    WaitInvocation wait1 = new WaitInvocation(1);    
    assertEquals(WaitInvocation.LONG, wait1.getSignature());
    assertEquals(1, wait1.getMillis());
    
    WaitInvocation wait2 = new WaitInvocation(2, 3);
    assertEquals(WaitInvocation.LONG_INT, wait2.getSignature());
    assertEquals(2, wait2.getMillis());
    assertEquals(3, wait2.getNanos());

    new WaitInvocation(0);
    new WaitInvocation(1);
    new WaitInvocation(Long.MAX_VALUE);

    try {
      new WaitInvocation(-1);
      fail("no exception thrown");
    } catch (IllegalArgumentException iae) {
      // expected
    }

    new WaitInvocation(0, 0);
    new WaitInvocation(1, 0);
    new WaitInvocation(Long.MAX_VALUE, 0);
    new WaitInvocation(Long.MAX_VALUE, Integer.MAX_VALUE);

    try {
      new WaitInvocation(-1, 0);
      fail("no exception thrown");
    } catch (IllegalArgumentException iae) {
      // expected
    }

    try {
      new WaitInvocation(-1, -1);
      fail("no exception thrown");
    } catch (IllegalArgumentException iae) {
      // expected
    }

    try {
      new WaitInvocation(0, -1);
      fail("no exception thrown");
    } catch (IllegalArgumentException iae) {
      // expected
    }
  }

}