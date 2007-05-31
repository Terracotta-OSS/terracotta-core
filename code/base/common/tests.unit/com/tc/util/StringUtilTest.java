/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import junit.framework.TestCase;

public final class StringUtilTest extends TestCase {

  public void testExists() {
    String[] testVals = new String[] { "test1", "test2", "test3", null };
    assertTrue(StringUtil.exists(testVals, "test1"));
    assertTrue(StringUtil.exists(testVals, null));
    assertFalse(StringUtil.exists(testVals, "Not in test vals"));
    assertFalse(StringUtil.exists(null, "can't examine null list"));
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

  public void testOrdinal() throws Exception {
    final String[] ordinals = { "0th", "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th", "11th",
        "12th", "13th", "14th", "15th", "16th", "17th", "18th", "19th", "20th", "21st", "22nd", "23rd", "24th", "25th",
        "26th", "27th", "28th", "29th", "30th", "31st", "32nd", "33rd", "34th", "35th", "36th", "37th", "38th", "39th",
        "40th", "41st", "42nd", "43rd" };
    for (int pos = 0; pos < ordinals.length; ++pos) {
      assertEquals(ordinals[pos], StringUtil.ordinal(pos));
    }
    // Some one off awkward values
    assertEquals("111th", StringUtil.ordinal(111));
    assertEquals("1011th", StringUtil.ordinal(1011));
    assertEquals("1911th", StringUtil.ordinal(1911));
    assertEquals("10011th", StringUtil.ordinal(10011));
    assertEquals("10911th", StringUtil.ordinal(10911));

    assertEquals("112th", StringUtil.ordinal(112));
    assertEquals("1012th", StringUtil.ordinal(1012));
    assertEquals("1912th", StringUtil.ordinal(1912));
    assertEquals("10012th", StringUtil.ordinal(10012));
    assertEquals("10912th", StringUtil.ordinal(10912));

    assertEquals("113th", StringUtil.ordinal(113));
    assertEquals("1013th", StringUtil.ordinal(1013));
    assertEquals("1913th", StringUtil.ordinal(1913));
    assertEquals("10013th", StringUtil.ordinal(10013));
    assertEquals("10913th", StringUtil.ordinal(10913));
  }

  public void testToString() {
    Long[] vals = new Long[] { new Long(1), new Long(2), new Long(3) };
    final String actual = StringUtil.toString(vals);
    assertEquals("1, 2, 3", actual);
  }
}
