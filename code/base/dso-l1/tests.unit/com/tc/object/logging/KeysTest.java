/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.logging;

import junit.framework.TestCase;

public class KeysTest extends TestCase {

  public void test() {
    String[] keys = Keys.getKeys(Class1.class);

    assertEquals(5, keys.length);

    for (int i = 0; i < keys.length; i++) {
      assertEquals("index " + i, "good", keys[i]);
    }

  }

  private static class Class1 {
    static final String           GOOD_1    = "good";
    public static final String    GOOD_2    = "good";
    protected static final String GOOD_3    = "good";
    private static final String   GOOD_4    = "good";
    transient static final String GOOD_5    = "good";

    private static final Object   NO_GOOD_1 = new Object(); // not type String
    private static String         NO_GOOD_2 = "bad";       // not final
    final String                  NO_GOOD_3 = "bad";       // not static
    static final String           No_GOOD_4 = "bad";       // not all caps

    public void silenceWarnings() {
      // this method here to make eclipse shutup about unused variables
      if (true) { throw new Error("oh no you didn't!"); }

      System.out.println(GOOD_4);
      System.out.println(NO_GOOD_1);
      System.out.println(NO_GOOD_2);
    }

  }

}
