/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import junit.framework.TestCase;

public class IntListTest extends TestCase {

  public void testBasic() throws Exception {

    int num = 5000000;
    IntList il = new IntList();

    for (int i = 0; i < num; i++) {
      il.add(i);

    }

    if (il.size() != num) { throw new AssertionError("wrong size reported: " + il.size()); }

    for (int i = 0; i < num; i++) {
      int val = il.get(i);
      if (val != i) { throw new AssertionError("Expected " + i + " got " + val); }
    }

    int[] array = il.toArray();
    if (array.length != num) { throw new AssertionError("array wrong size: " + array.length); }
    for (int i = 0; i < array.length; i++) {
      if (i != array[i]) { throw new AssertionError(); }
    }
  }

}
