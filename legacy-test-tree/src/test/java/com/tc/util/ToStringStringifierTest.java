/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

/**
 * Unit test for {@link ToStringStringifier}.
 */
public class ToStringStringifierTest extends TCTestCase {

  private static class MyObj {
    private final String value;

    public MyObj(String value) {
      this.value = value;
    }

    public String toString() {
      return "XXX" + this.value + "YYY";
    }
  }

  public void testNull() {
    // Make sure we can disambiguate (new String("null")) and null.
    String nullAsString = ToStringStringifier.INSTANCE.toString(null);
    assertFalse(nullAsString.equals("null"));
    assertTrue(nullAsString.trim().length() > 0);
  }

  public void testStringification() {
    assertEquals("XXXYYY", ToStringStringifier.INSTANCE.toString(new MyObj("")));
    assertEquals("XXX   YYY", ToStringStringifier.INSTANCE.toString(new MyObj("   ")));
    assertEquals("XXXaaabbbYYY", ToStringStringifier.INSTANCE.toString(new MyObj("aaabbb")));
  }

}