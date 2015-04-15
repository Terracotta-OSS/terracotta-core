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

  public void testToString() {
    IntList list = new IntList();

    assertEquals("{}", list.toString());

    list.add(1);
    assertEquals("{1}", list.toString());

    list.add(11);
    assertEquals("{1, 11}", list.toString());

    list.add(21);
    assertEquals("{1, 11, 21}", list.toString());

    list.add(1211);
    list.add(111221);
    assertEquals("{1, 11, 21, 1211, 111221}", list.toString());

    // what's the next number in the sequence above?
  }

}
