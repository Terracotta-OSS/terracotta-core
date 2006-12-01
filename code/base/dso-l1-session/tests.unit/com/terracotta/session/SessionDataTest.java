/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session;

import java.util.Arrays;

import junit.framework.TestCase;

public class SessionDataTest extends TestCase {

  public final void testConstructor() {
    final String id = "SomeSessionId";
    final int maxIdleSeconds = 123;
    SessionData sd = new SessionData(id, maxIdleSeconds);
    assertEquals(maxIdleSeconds, sd.getMaxInactiveMillis() / 1000);
  }

  public final void testCollection() {
    final String id = "SomeSessionId";
    final int maxIdleSeconds = 123;
    SessionData sd = new SessionData(id, maxIdleSeconds);
    final String[] attributes = new String[] { "one", "two", "three", "four", "five" };
    for (int i = 0; i < attributes.length; i++) {
      String a = attributes[i];

      // test set/get
      sd.setAttribute(a, a);
      String v = (String) sd.getAttribute(a);
      assertSame(a, v);

      // test attribute names
      String[] namesOut = sd.getAttributeNames();
      Arrays.sort(namesOut);
      assertEquals(i + 1, namesOut.length);
      for (int j = 0; j < i; j++)
        assertTrue(Arrays.binarySearch(namesOut, attributes[j]) >= 0);

      // test replace/get
      final String newVal = new String("SomeNewString");
      final String oldVal = (String) sd.setAttribute(a, newVal);
      assertSame(a, oldVal);
      assertSame(newVal, sd.getAttribute(a));

      // test remove
      final String removedVal = (String) sd.removeAttribute(a);
      assertSame(newVal, removedVal);
      assertNull(sd.removeAttribute(a));

      // put it back for further testing...
      sd.setAttribute(a, a);
    }
  }
}
