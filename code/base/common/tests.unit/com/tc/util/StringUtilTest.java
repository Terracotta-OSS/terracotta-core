/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import junit.framework.TestCase;

public final class StringUtilTest extends TestCase {

  public void testSafeToString() {
    assertEquals(StringUtil.NULL_STRING, StringUtil.safeToString(null));
    assertEquals("10", StringUtil.safeToString(Integer.valueOf(10)));
  }

  public void testIndentLinesNegativeIndent() {
    try {
      StringUtil.indentLines(new StringBuffer(), -10, ' ');
      Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

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

  public void testIndentLinesStringNull() {
    try {
      StringUtil.indentLines((String) null, 5);
      fail("Expected NPE");
    } catch (NullPointerException e) {
      // expected exception
    }
  }

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

  public void testToString() {
    Long[] vals = new Long[] { Long.valueOf(1), Long.valueOf(2), Long.valueOf(3) };
    final String actual = StringUtil.toString(vals);
    assertEquals("1, 2, 3", actual);
  }

  public void testToPaddedString() {
    // Too big for padding
    assertEquals("123", StringUtil.toPaddedString(123, 10, 1));

    // Pad out with 0's
    assertEquals("0000000123", StringUtil.toPaddedString(123, 10, 10));

    // Base 16
    assertEquals("00cd", StringUtil.toPaddedString(205, 16, 4));
  }

}
