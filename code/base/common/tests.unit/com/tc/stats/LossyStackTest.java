/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import junit.framework.TestCase;

public class LossyStackTest extends TestCase {

  public void testException() {
    try {
      new LossyStack(0);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }

    try {
      new LossyStack(-4);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }

  }

  public void test() {
    LossyStack stack = new LossyStack(5);
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

    Integer[] data = (Integer[]) stack.toArray(new Integer[stack.depth()]);
    assertEquals(5, data.length);
    for (int i = 0; i < data.length; i++) {
      assertEquals(Integer.valueOf(7 - i), data[i]);
    }

  }

}
