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
package com.tc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public final class StringUtilTest {

  @Test
  public void testSafeToString() {
    assertEquals(StringUtil.NULL_STRING, StringUtil.safeToString(null));
    assertEquals("10", StringUtil.safeToString(Integer.valueOf(10)));
  }

  @Test
  public void testIndentLinesNegativeIndent() {
    try {
      StringUtil.indentLines(new StringBuffer(), -10, ' ');
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testIndentLines() {
    // null StringBuffer -> null result
    assertEquals(null, StringUtil.indentLines(null, 2, ' '));

    // 0 indent -> unchanged
    String start = "abc\ndef\nghi";
    StringBuffer sb = new StringBuffer(start);
    assertEquals(start, StringUtil.indentLines(sb, 0, ' ').toString());
    assertEquals(start, sb.toString());

    // check indent after line breaks
    String after = "\t\tabc\n\t\tdef\n\t\tghi";
    assertEquals(after, StringUtil.indentLines(sb, 2, '\t').toString());
    assertEquals(after, sb.toString());
  }

  @Test
  public void testIndentLinesStringNull() {
    try {
      StringUtil.indentLines((String) null, 5);
      fail("Expected NPE");
    } catch (NullPointerException e) {
      // expected exception
    }
  }

  @Test
  public void testToStringObjectArrayStringStringString() {
    // Test 1, all nulls
    String expected = StringUtil.NULL_STRING;
    String rv = StringUtil.toString((Object[]) null, null, null, null);
    assertNotNull("StringUtil.toString(Object[],String,String,String) returned null", rv);
    assertEquals("Returned string was not the same as expected", expected, rv);

    // Test 2, objects with no formatting
    Object[] arr = new Object[] { "one", "two", "three" };
    expected = "onetwothree";
    rv = StringUtil.toString(arr, null, null, null);
    assertNotNull("StringUtil.toString(Object[],String,String,String) returned null", rv);
    assertEquals("Returned string was not the same as expected", expected, rv);

    // Test 3, objects with some formatting
    expected = "[one:[two:[three";
    rv = StringUtil.toString(arr, ":", "[", null);
    assertNotNull("StringUtil.toString(Object[],String,String,String) returned null", rv);
    assertEquals("Returned string was not the same as expected", expected, rv);

    // Test 4, objects with all formatting
    expected = "<one>,<two>,<three>";
    rv = StringUtil.toString(arr, ",", "<", ">");
    assertNotNull("StringUtil.toString(Object[],String,String,String) returned null", rv);
    assertEquals("Returned string was not the same as expected", expected, rv);

    // Test 5, objects with all formatting and null elements
    arr = new Object[] { "one", null, null, "four" };
    expected = "<one>,<null>,<null>,<four>";
    rv = StringUtil.toString(arr, ",", "<", ">");
    assertNotNull("StringUtil.toString(Object[],String,String,String) returned null", rv);
    assertEquals("Returned string was not the same as expected", expected, rv);
  }

  @Test
  public void testToString() {
    Long[] vals = new Long[] { Long.valueOf(1), Long.valueOf(2), Long.valueOf(3) };
    final String actual = StringUtil.toString(vals);
    assertEquals("1, 2, 3", actual);
  }

  @Test
  public void testToPaddedString() {
    // Too big for padding
    assertEquals("123", StringUtil.toPaddedString(123, 10, 1));

    // Pad out with 0's
    assertEquals("0000000123", StringUtil.toPaddedString(123, 10, 10));

    // Base 16
    assertEquals("00cd", StringUtil.toPaddedString(205, 16, 4));
  }

}
