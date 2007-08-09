/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * A general purpose assertion utility. By default it is on, but you can disable the throwing of exceptions by giving
 * the system property "tcassert" a value of 'false'.
 */
public class Assert {

  private static final String  ASSERT_PROPERTY_NAME = "tcassert";

  // When (if) we want to run *without* assertions enabled by default, use the line below to initialize instead
  // private static final boolean enabled = Boolean.getBoolean(ASSERT_PROPERTY_NAME);
  //
  // NOTE: We need to be VERY careful about casually turning off assertions. It's one thing to make the assertions not
  // throw errors (which the current disable/enable mechanism does). It's entirely something different to remove the
  // calls to assertions. At the time of this writing, there are state modifying method calls in the code base that are
  // paremeters to these assert method. Removing the call altogher would most certainly change the logic of the system
  // in potentially silent and catastropic ways
  private static final boolean enabled              = Boolean.valueOf(System.getProperty(ASSERT_PROPERTY_NAME, "true"))
                                                        .booleanValue();

  private static boolean isEnabled() {
    return enabled;
  }

  // This returns an exception, instead of throwing one, so that you can do (e.g.):
  // public Object foo() { throw Assert.failure("doesn't work"); }
  // or whatever. If this just threw the exception itself, the compiler would complain
  // (above) that there's no value being returned.
  public static TCAssertionError failure(Object message, Throwable t) {
    return new TCAssertionError(StringUtil.safeToString(message), t);
  }

  public static TCAssertionError failure(Object message) {
    return new TCAssertionError(StringUtil.safeToString(message));
  }

  public static void eval(boolean expr) {
    if ((!expr) && isEnabled()) { throw failure("Assertion failed"); }
    return;
  }

  public static void eval(Object message, boolean expr) {
    if ((!expr) && isEnabled()) { throw failure("Assertion failed: " + StringUtil.safeToString(message)); }
    return;
  }

  public static void assertTrue(boolean expr) {
    eval(expr);
  }

  public static void assertTrue(Object message, boolean expr) {
    eval(message, expr);
  }

  public static void assertFalse(boolean expr) {
    eval(!expr);
  }

  public static void assertFalse(Object message, boolean expr) {
    eval(message, !expr);
  }

  public static void assertNull(Object o) {
    assertNull("object", o);
  }

  public static void assertNull(Object what, Object o) {
    if ((o != null) && isEnabled()) { throw failure(StringUtil.safeToString(what) + " was not null"); }
  }

  public static void assertNotNull(Object what, Object o) {
    if ((o == null) && isEnabled()) { throw new NullPointerException(StringUtil.safeToString(what) + " is null"); }
  }

  public static void assertNotNull(Object o) {
    assertNotNull("object", o);
  }

  // validate that the given (1 dimensional) array of references contains no nulls
  public static void assertNoNullElements(Object[] array) {
    if (!isEnabled()) return;
    assertNotNull(array);

    for (int i = 0; i < array.length; i++) {
      assertNotNull("item " + i, array[i]);
    }
  }

  public static void assertNoBlankElements(String[] array) {
    if (!isEnabled()) return;
    assertNotNull(array);

    for (int i = 0; i < array.length; ++i)
      assertNotBlank(array[i]);
  }

  public static void assertNotEmpty(Object what, String s) {
    assertNotNull(what, s);
    if ((s.length() == 0) && isEnabled()) throw new IllegalArgumentException(StringUtil.safeToString(what)
                                                                             + " is empty");
  }

  public static void assertNotEmpty(String s) {
    assertNotEmpty("string", s);
  }

  public static void assertNotBlank(Object what, String s) {
    assertNotEmpty(what, s);
    if ((s.trim().length() == 0) && isEnabled()) throw new IllegalArgumentException(StringUtil.safeToString(what)
                                                                                    + " is blank");
  }

  public static void assertNotBlank(String s) {
    assertNotBlank("string", s);
  }

  public static void assertSame(Object lhs, Object rhs) {
    if (lhs == null) {
      eval("leftHandSide == null, but rightHandSide != null", rhs == null);
    } else {
      eval("leftHandSide != null, but rightHandSide == null", rhs != null);
      eval("leftHandSide != rightHandSide", lhs == rhs);
    }
  }

  public static void assertEquals(int expected, int actual) {
    if (expected != actual) { throw new TCAssertionError("Expected <" + expected + "> but got <" + actual + ">"); }
  }

  public static void assertEquals(Object msg, int expected, int actual) {
    if (expected != actual) { throw new TCAssertionError(msg.toString() + ": Expected <" + expected + "> but got <" + actual + ">"); }
  }
  
  public static void assertEquals(double expected, double actual) {
    if (expected != actual) { throw new TCAssertionError("Expected <" + expected + "> but got <" + actual + ">"); }
  }
  
  public static void assertEquals(double expected, double actual, double epsilon) {
    if (Math.abs(actual - expected) > Math.abs(epsilon)) { throw new TCAssertionError("Expected <" + expected + "> but got <" + actual + ">"); }
  }

  public static void assertEquals(boolean expected, boolean actual) {
    if (expected != actual) { throw new TCAssertionError("Expected <" + expected + "> but got <" + actual + ">"); }
  }

  public static void assertEquals(byte[] expected, byte[] actual) {
    boolean expr = (expected == null) ? actual == null : Arrays.equals(expected, actual);
    if (!expr) { throw new TCAssertionError("Expected <" + expected + "> but got <" + actual + ">"); }
  }

  public static void assertEquals(Object expected, Object actual) {
    boolean expr = (expected == null) ? actual == null : expected.equals(actual);
    if (!expr) { throw new TCAssertionError("Expected <" + expected + "> but got <" + actual + ">"); }
  }

  public static void assertConsistentCollection(Collection collection, Class elementClass, boolean allowNullElements) {
    assertNotNull("Collection", collection);
    assertNotNull("Element class", elementClass);
    for (Iterator pos = collection.iterator(); pos.hasNext();) {
      Object element = pos.next();
      if (!allowNullElements) {
        assertNotNull(element);
      }
      if (element != null) {
        eval("Element '" + element + "' is not an instance of '" + elementClass.getName() + "'", elementClass
            .isInstance(element));
      }
    }
  }

  /**
   * Tests for equality using the <code>==</code> operator, <em>not</em> <code>Object.equals(Object)</code>.
   * <code>null</code> is a valid element.
   */
  public static void assertContainsElement(Object[] objectArray, Object requiredElement) {
    assertNotNull(objectArray);
    for (int pos = 0; pos < objectArray.length; pos++) {
      if (objectArray[pos] == requiredElement) return;
    }
    throw failure("Element<" + requiredElement + "> not found in array "
                  + StringUtil.toString(objectArray, ",", "<", ">"));
  }

  public static void fail() {
    throw failure("generic failure");
  }
  
  public static void pre(boolean v){
    if (!v) throw new TCAssertionError("Precondition failed");
  }

  public static void post(boolean v){
    if (!v) throw new TCAssertionError("Postcondition failed");
  }
  public static void inv(boolean v){
    if (!v) throw new TCAssertionError("Invariant failed");
  }
}