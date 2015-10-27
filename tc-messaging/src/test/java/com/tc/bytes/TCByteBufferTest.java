/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TCByteBufferTest {

  @Test
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
