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
package com.tc.object.tx;

import junit.framework.TestCase;

public class WaitInvocationTest extends TestCase {

  @SuppressWarnings("unused")
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
