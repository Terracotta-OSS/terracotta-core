/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TCByteBufferTest {

  @Test
  public void testUint() {
    TCByteBuffer buf = TCByteBufferFactory.getInstance(4);

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
