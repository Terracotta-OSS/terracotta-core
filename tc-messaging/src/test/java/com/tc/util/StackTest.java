/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;
import java.util.Random;

import org.junit.Test;

public class StackTest {

  @Test
  public void test() throws Exception {
    Stack<Object> tcStack = new Stack<Object>();
    java.util.Stack<Object> javaStack = new java.util.Stack<Object>();

    doRandomTests(tcStack, javaStack);
  }

  private void doRandomTests(Stack<Object> tcStack, java.util.Stack<Object> javaStack) {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    Random r = new Random(seed);
    try {
      int count = 10000;
      while (count-- > 0) {
        switch (r.nextInt(6)) {
          case 0:
            callPop(tcStack, javaStack);
            break;
          case 1:
            callPush(tcStack, javaStack, r);
            break;
          case 2:
            callPeek(tcStack, javaStack);
            break;
          case 3:
            callEmpty(tcStack, javaStack);
            break;
          case 4:
            callSize(tcStack, javaStack);
            break;
          case 5:
            callSearch(tcStack, javaStack, r);
            break;
          default:
            throw new AssertionError("Should never get here.");
        }
      }
    } catch (AssertionError e) {
      throw new AssertionError("Failure with seed " + seed + " " + e);
    }
  }

  private void callSearch(Stack<Object> tcStack, java.util.Stack<Object> javaStack, Random r) {
    Integer subject = Integer.valueOf(r.nextInt(10000));
    assertEquals(javaStack.search(subject), tcStack.search(subject));
  }

  private void callSize(Stack<Object> tcStack, java.util.Stack<Object> javaStack) {
    assertEquals(javaStack.size(), tcStack.size());
  }

  private void callEmpty(Stack<Object> tcStack, java.util.Stack<Object> javaStack) {
    assertEquals(javaStack.empty(), tcStack.empty());
  }

  private void callPush(Stack<Object> tcStack, java.util.Stack<Object> javaStack, Random r) {
    Integer subject = Integer.valueOf(r.nextInt(10000));
    assertEquals(javaStack.push(subject), tcStack.push(subject));
    assertEquals(javaStack.size(), tcStack.size());
  }

  private void callPeek(Stack<Object> tcStack, java.util.Stack<Object> javaStack) {
    boolean thrownException = false;
    Object jo = null, to = null;
    Exception je = null, te = null;
    try {
      jo = javaStack.peek();
    } catch (Exception ex) {
      je = ex;
      thrownException = true;
    }
    try {
      to = tcStack.peek();
      assertFalse(thrownException);
    } catch (Exception ex) {
      assertTrue(thrownException);
      te = ex;
    }
    assertEquals(jo, to);
    if (je != null) {
      assertEquals(je.toString(), te.toString());
    }
  }

  private void callPop(Stack<Object> tcStack, java.util.Stack<Object> javaStack) {
    boolean thrownException = false;
    Object jo = null, to = null;
    Exception je = null, te = null;
    try {
      jo = javaStack.pop();
    } catch (Exception ex) {
      je = ex;
      thrownException = true;
    }
    try {
      to = tcStack.pop();
      assertFalse(thrownException);
    } catch (Exception ex) {
      assertTrue(thrownException);
      te = ex;
    }
    assertEquals(jo, to);
    if (je != null) {
      assertEquals(je.toString(), te.toString());
    }
  }

}
