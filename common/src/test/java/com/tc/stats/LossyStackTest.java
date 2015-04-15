/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.stats;

import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

import junit.framework.TestCase;

@Category(CheckShorts.class)
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
