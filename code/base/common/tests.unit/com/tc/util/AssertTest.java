/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

import java.util.ArrayList;

/**
 * Unit test for {@link Assert}.
 */
public class AssertTest extends TCTestCase {
  
  public void testAssertNoBlankElements() {
    try {
      Assert.assertNoBlankElements(null);
      fail("Didn't get NPE on no array");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      Assert.assertNoBlankElements(new String[] { "x", "y", null, "z" });
      fail("Didn't get NPE on null element");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      Assert.assertNoBlankElements(new String[] { "x", "y", "", "z" });
      fail("Didn't get IAE on empty string");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      Assert.assertNoBlankElements(new String[] { "x", "y", "   ", "z" });
      fail("Didn't get IAE on blank string");
    } catch (IllegalArgumentException iae) {
      // ok
    }
  }

  public void testAssertNoNullElements() {
    try {
      Assert.assertNoNullElements(null);
      fail("null passed check");
    } catch (NullPointerException npe) {
      // ok
    }

    Assert.assertNoNullElements(new Object[] {});

    Object[] values = new Object[] { new Object(), null };

    try {
      Assert.assertNoNullElements(values);
      fail("array with null passed check");
    } catch (NullPointerException npe) {
      // ok
    }

    values[1] = new Object();

    Assert.assertNoNullElements(values);
  }

  public void testEval() throws Exception {
    Assert.eval(true);
    Assert.eval("foo", true);

    try {
      Assert.eval(false);
      fail("Didn't get exception on eval(false)");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      Assert.eval("foo", false);
      fail("Didn't get exception on eval(\"foo\", false)");
    } catch (TCAssertionError tcae) {
      assertTrue(tcae.getMessage().indexOf("foo") >= 0);
    }
  }

  public void testFail() throws Exception {
    TCAssertionError error = Assert.failure("foo");
    assertTrue(error.getMessage().indexOf("foo") >= 0);
  }

  public void testAssertNotNull() throws Exception {
    Assert.assertNotNull(new Object());
    Assert.assertNotNull("foo", new Object());

    try {
      Assert.assertNotNull(null);
      fail("Didn't get exception on assertNotNull(null)");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      Assert.assertNotNull("foo", null);
      fail("Didn't get exception on assertNotNull(\"foo\", null)");
    } catch (NullPointerException npe) {
      assertTrue(npe.getMessage().indexOf("foo") >= 0);
    }
  }

  public void testAssertNull() throws Exception {
    Assert.assertNull(null);
    Assert.assertNull("foo", null);

    try {
      Assert.assertNull(new Object());
      fail("Didn't get assertion");
    } catch (TCAssertionError e) {
      // expected
    }

    try {
      Assert.assertNull("foo", new Object());
      fail("Didn't get assertion");
    } catch (TCAssertionError e) {
      // expected
    }
  }

  public void testAssertNotEmpty() throws Exception {
    Assert.assertNotEmpty("x");
    Assert.assertNotEmpty("foo", "x");

    try {
      Assert.assertNotEmpty(null);
      fail("Didn't get exception on assertNotEmpty(null)");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      Assert.assertNotEmpty("");
      fail("Didn't get exception on assertNotEmpty(\"\")");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      Assert.assertNotEmpty("foo", null);
      fail("Didn't get exception on assertNotEmpty(\"foo\", null)");
    } catch (NullPointerException npe) {
      assertTrue(npe.getMessage().indexOf("foo") >= 0);
    }

    try {
      Assert.assertNotEmpty("foo", "");
      fail("Didn't get exception on assertNotEmpty(\"foo\", \"\")");
    } catch (IllegalArgumentException iae) {
      assertTrue(iae.getMessage().indexOf("foo") >= 0);
    }
  }

  public void testAssertNotBlank() throws Exception {
    Assert.assertNotBlank("x");
    Assert.assertNotBlank("foo", "x");

    try {
      Assert.assertNotBlank(null);
      fail("Didn't get exception on assertNotBlank(null)");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      Assert.assertNotBlank("");
      fail("Didn't get exception on assertNotBlank(\"\")");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      Assert.assertNotBlank("   ");
      fail("Didn't get exception on assertNotBlank(\"   \")");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      Assert.assertNotBlank("foo", null);
      fail("Didn't get exception on assertNotBlank(\"foo\", null)");
    } catch (NullPointerException npe) {
      assertTrue(npe.getMessage().indexOf("foo") >= 0);
    }

    try {
      Assert.assertNotBlank("foo", "");
      fail("Didn't get exception on assertNotBlank(\"foo\", \"\")");
    } catch (IllegalArgumentException iae) {
      assertTrue(iae.getMessage().indexOf("foo") >= 0);
    }

    try {
      Assert.assertNotBlank("foo", "   ");
      fail("Didn't get exception on assertNotBlank(\"foo\", \"   \")");
    } catch (IllegalArgumentException iae) {
      assertTrue(iae.getMessage().indexOf("foo") >= 0);
    }
  }

  public final void testAssertContainsElement() throws Exception {
    try {
      Assert.assertContainsElement(null, null);
      fail("Should not be able to pass null array");
    } catch (NullPointerException npe) {
      // Expected
    }
    String foo = "foo";
    Object[] objectArray = new Object[] { new Object(), foo, null, new Integer(1), new ArrayList() };
    for (int pos = 0; pos < objectArray.length; pos++) {
      Assert.assertContainsElement(objectArray, objectArray[pos]);
    }
    try {
      Assert.assertContainsElement(objectArray, new String(foo));
      throw new RuntimeException("Array does not contain the specified element");
    } catch (TCAssertionError tcae) {
      // Expected
    }
  }

}
