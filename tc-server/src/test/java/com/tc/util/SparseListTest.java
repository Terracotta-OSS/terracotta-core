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
package com.tc.util;

import com.tc.test.TCTestCase;


public class SparseListTest extends TCTestCase {
  public SparseListTest() {
  }

  public void testIterateEmpty() {
    SparseList<String> list = new SparseList<>();
    int count = 0;
    for (String string : list) {
      fail("Non-existent: " + string);
      count += 1;
    }
    assertEquals(0, count);
  }

  public void testIterateOneAtZero() {
    SparseList<String> list = new SparseList<>();
    list.insert(0, "zero");
    int count = 0;
    for (String string : list) {
      assertEquals("zero", string);
      count += 1;
    }
    assertEquals(1, count);
  }

  public void testIterateOneAtTen() {
    SparseList<String> list = new SparseList<>();
    list.insert(10, "ten");
    int count = 0;
    for (String string : list) {
      assertEquals("ten", string);
      count += 1;
    }
    assertEquals(1, count);
  }

  public void testIterateCommon() {
    SparseList<String> list = new SparseList<>();
    String[] checks = {
                       "zero",
                       "one",
                       "two",
                       "three",
                       "four",
                       "five",
    };
    list.insert(5, checks[5]);
    list.insert(1, checks[1]);
    list.insert(2, checks[2]);
    list.insert(0, checks[0]);
    list.insert(3, checks[3]);
    list.insert(4, checks[4]);
    int count = 0;
    for (String string : list) {
      assertEquals(checks[count], string);
      count += 1;
    }
    assertEquals(6, count);
  }

  public void testReplaceAndIterate() {
    SparseList<String> list = new SparseList<>();
    String replaced = list.insert(10, "ten");
    assertNull(replaced);
    replaced = list.insert(9, "nine");
    assertNull(replaced);
    replaced = list.insert(10, "ten-2");
    assertEquals("ten", replaced);
    
    String checks[] = {"nine", "ten-2"};
    int count = 0;
    for (String string : list) {
      assertEquals(checks[count], string);
      count += 1;
    }
    assertEquals(2, count);
  }
}