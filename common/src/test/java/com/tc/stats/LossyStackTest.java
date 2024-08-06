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
package com.tc.stats;

import junit.framework.TestCase;

public class LossyStackTest extends TestCase {

  @SuppressWarnings("unused")
  public void testException() {
    try {
      new LossyStack<Object>(0);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }

    try {
      new LossyStack<Object>(-4);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }
  }

  public void test() {
    LossyStack<Integer> stack = new LossyStack<Integer>(5);
    assertEquals(0, stack.depth());
    assertTrue(stack.isEmtpy());
    assertNull(stack.peek());

    try {
      stack.pop();
      fail();
    } catch (IllegalStateException ise) {
      // expected
    }

    stack.push(Integer.valueOf(1));
    assertFalse(stack.isEmtpy());
    assertEquals(1, stack.depth());
    stack.push(Integer.valueOf(2));
    assertFalse(stack.isEmtpy());
    assertEquals(2, stack.depth());

    assertEquals(Integer.valueOf(2), stack.pop());
    assertFalse(stack.isEmtpy());
    assertEquals(Integer.valueOf(1), stack.pop());
    assertEquals(0, stack.depth());
    assertTrue(stack.isEmtpy());
    assertNull(stack.peek());

    stack.push(Integer.valueOf(1));
    stack.push(Integer.valueOf(2));
    stack.push(Integer.valueOf(3));
    stack.push(Integer.valueOf(4));
    stack.push(Integer.valueOf(5));
    assertEquals(5, stack.depth());
    stack.push(Integer.valueOf(6));
    assertEquals(5, stack.depth());
    stack.push(Integer.valueOf(7));
    assertEquals(5, stack.depth());

    Integer[] data = stack.toArray(new Integer[stack.depth()]);
    assertEquals(5, data.length);
    for (int i = 0; i < data.length; i++) {
      assertEquals(Integer.valueOf(7 - i), data[i]);
    }

  }

}
