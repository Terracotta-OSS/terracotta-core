/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import java.util.NoSuchElementException;

import junit.framework.TestCase;

public class StringArrayEnumerationTest extends TestCase {

  public void testNullArray() {
    StringArrayEnumeration sae = null;
    try {
      sae = new StringArrayEnumeration(null);
    } catch (Exception e) {
      e.printStackTrace();
      fail("unexpected Exception: " + e);
    }
    assertFalse(sae.hasMoreElements());
    try {
      sae.nextElement();
      fail("expected exception");
    } catch (NoSuchElementException nsee) {
      // ok
    }
  }

  public void testEmptyArray() {
    StringArrayEnumeration sae = null;
    try {
      sae = new StringArrayEnumeration(new String[0]);
    } catch (Exception e) {
      e.printStackTrace();
      fail("unexpected Exception: " + e);
    }
    assertFalse(sae.hasMoreElements());
    try {
      sae.nextElement();
      fail("expected exception");
    } catch (NoSuchElementException nsee) {
      // ok
    }
  }

  public void testNonEmptyArray() {
    final String[] array = new String[] { "1", "2", "3" };
    StringArrayEnumeration sae = null;
    try {
      sae = new StringArrayEnumeration(array);
    } catch (Exception e) {
      e.printStackTrace();
      fail("unexpected Exception: " + e);
    }
    for (int i = 0; i < array.length; i++) {
      assertTrue(sae.hasMoreElements());
      String s = (String)sae.nextElement();
      assertEquals(array[i], s);
    }
    assertFalse(sae.hasMoreElements());
    try {
      sae.nextElement();
      fail("expected exception");
    } catch (NoSuchElementException nsee) {
      // ok
    }
  }

}
