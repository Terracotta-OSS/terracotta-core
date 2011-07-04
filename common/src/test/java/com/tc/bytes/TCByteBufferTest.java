/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bytes;

import junit.framework.TestCase;

public class TCByteBufferTest extends TestCase {

  public void testUint() {
    TCByteBuffer buf = TCByteBufferFactory.getInstance(false, 4);

    buf.putUint(0, 0);
    assertEquals(0, buf.getUint(0));

    buf.putUint(0, 1);
    assertEquals(1, buf.getUint(0));

    long highBit = 0x80000000L;
    buf.putUint(0, highBit);
    assertEquals(highBit, buf.getUint(0));

    final long max = 0xFFFFFFFFL;
    buf.putUint(0, max);
    assertEquals(max, buf.getUint(0));

    try {
      buf.putUint(0, max + 1);
      fail("I was allowed to write an illegal value");
    } catch (IllegalArgumentException iae) {
      // expected
    }

    try {
      buf.putUint(0, -1);
      fail("I was allowed to write an illegal value (-1)");
    } catch (IllegalArgumentException iae) {
      // expected
    }

  }
}
