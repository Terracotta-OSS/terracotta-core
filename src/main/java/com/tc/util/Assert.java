/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.Arrays;
import java.util.Collection;

/**
 * A general purpose assertion utility
 */
public class Assert {

  /**
   * This returns an exception, instead of throwing one, so that you can do (e.g.): <code>
   * public Object foo() { throw Assert.failure("doesn't work"); }
   * </code>
   * or whatever. If this just threw the exception itself, the compiler would complain (above) that there's no value
   * being returned.
   *
   * @param message The message to put in the assertion error
   * @param t The exception to wrap
   * @return New TCAssertionError, ready to throw
   */
  public static TCAssertionError failure(Object message, Throwable t) {
    return new TCAssertionError(StringUtil.safeToString(message), t);
  }

  /**
   * This returns an exception, instead of throwing one, so that you can do (e.g.): <code>
   * public Object foo() { throw Assert.failure("doesn't work"); }
   * </code>
   * or whatever. If this just threw the exception itself, the compiler would complain (above) that there's no value
   * being returned.
   *
   * @param message The message to put in the assertion error
   * @return New TCAssertionError, ready to throw
   */
  public static TCAssertionError failure(Object message) {
    return new TCAssertionError(StringUtil.safeToString(message));
  }

  /**
   * Evaluate the boolean exception and throw an assertion error if false
   *
   * @param expr Expression
   */
  public static void eval(boolean expr) {
    if (!expr) { throw failure("Assertion failed"); }
    return;
  }

  /**
   * Evaluate the boolean exception and throw an assertion error if false
   *
   * @param expr Expression
   * @param message Message for assertion error if false
   */
  public static void eval(Object message, boolean expr) {
    if (!expr) { throw failure("Assertion failed: " + StringUtil.safeToString(message)); }
    return;
  }

  /**
   * Evaluate the boolean exception and throw an assertion error if false
   *
   * @param expr Expression
   */
  public static void assertTrue(boolean expr) {
    eval(expr);
  }

  /**
   * Evaluate the boolean exception and throw an assertion error if false
   *
   * @param expr Expression
   * @param message Message for assertion error if false
   */
  public static void assertTrue(Object message, boolean expr) {
    eval(message, expr);
  }

  /**
   * Evaluate the boolean exception and throw an assertion error if true
   *
   * @param expr Expression
   * @param message Message for assertion error if true
   */
  public static void assertFalse(boolean expr) {
    eval(!expr);
  }

  /**
   * Evaluate the boolean exception and throw an assertion error if true
   *
   * @param expr Expression
   * @param message Message for assertion error if true
   */
  public static void assertFalse(Object message, boolean expr) {
    eval(message, !expr);
  }

  /**
   * If o is non-null, throw assertion error
   *
   * @param o Object
   */
  public static void assertNull(Object o) {
    assertNull("object", o);
  }

  /**
   * If o is non-null, throw assertion error
   *
   * @param o Object
   * @param what Message for error
   */
  public static void assertNull(Object what, Object o) {
    if (o != null) { throw failure(StringUtil.safeToString(what) + " was not null"); }
  }

  /**
   * If o is null, throw assertion error with message what
   *
   * @param o Object
   * @param what Message for error
   */
  public static void assertNotNull(Object what, Object o) {
    if (o == null) { throw new NullPointerException(StringUtil.safeToString(what) + " is null"); }
  }

  /**
   * If o is null, throw assertion error
   *
   * @param o Object
   */
  public static void assertNotNull(Object o) {
    assertNotNull("object", o);
  }

  /**
   * Validate that the given (1 dimensional) array of references contains no nulls
   *
   * @param array Array
   */
  public static void assertNoNullElements(Object[] array) {
    assertNotNull(array);

    for (int i = 0; i < array.length; i++) {
      assertNotNull("item " + i, array[i]);
    }
  }

  /**
   * Validate that the given array of strings contains no nulls or empty strings
   *
   * @param array Array of strings
   */
  public static void assertNoBlankElements(String[] array) {
    assertNotNull(array);

    for (String s : array)
      assertNotBlank(s);
  }

  /**
   * Validate that s is not null or empty and throw what as a message
   *
   * @param s String
   * @param what Message
   */
  public static void assertNotEmpty(Object what, String s) {
    assertNotNull(what, s);
    if (s.length() == 0) throw new IllegalArgumentException(StringUtil.safeToString(what)
                                                                             + " is empty");
  }

  /**
   * Validate that s is not null or empty
   *
   * @param s String
   */
  public static void assertNotEmpty(String s) {
    assertNotEmpty("string", s);
  }

  /**
   * Validate that s is not blank and throw what as a message
   *
   * @param s String
   * @param what Message
   */
  public static void assertNotBlank(Object what, String s) {
    assertNotEmpty(what, s);
    if (s.trim().length() == 0) throw new IllegalArgumentException(StringUtil.safeToString(what)
                                                                                    + " is blank");
  }

  /**
   * Validate that s is not blank
   *
   * @param s String
   */
  public static void assertNotBlank(String s) {
    assertNotBlank("string", s);
  }

  /**
   * Validate that lhs and rhs are identical object references or both are null
   *
   * @param lhs Left hand side
   * @param rhs Right hand side
   */
  public static void assertSame(Object lhs, Object rhs) {
    if (lhs == null) {
      eval("leftHandSide == null, but rightHandSide != null", rhs == null);
    } else {
      eval("leftHandSide != null, but rightHandSide == null", rhs != null);
      eval("leftHandSide != rightHandSide", lhs == rhs);
    }
  }

  /**
   * Assert expected and actual values are equal
   *
   * @param expected Expected value
   * @param actual Actual value
   */
  public static void assertEquals(int expected, int actual) {
    if (expected != actual) { throw new TCAssertionError("Expected <" + expected + "> but got <" + actual + ">"); }
  }

  /**
   * Assert expected and actual values are equal
   *
   * @param expected Expected value
   * @param actual Actual value
   */
  public static void assertEquals(long expected, long actual) {
    if (expected != actual) { throw new TCAssertionError("Expected <" + expected + "> but got <" + actual + ">"); }
  }

  /**
   * Assert expected and actual values are equal and return what as a message
   *
   * @param expected Expected value
   * @param actual Actual value
   * @param msg Message, should be non-null
   */
  public static void assertEquals(Object msg, int expected, int actual) {
    if (expected != actual) { throw new TCAssertionError(msg + ": Expected <" + expected + "> but got <"
                                                                        + actual + ">"); }
  }

  /**
   * Assert expected and actual values are equal
   *
   * @param expected Expected value
   * @param actual Actual value
   */
  public static void assertEquals(double expected, double actual) {
    if (expected != actual) { throw new TCAssertionError("Expected <" + expected + "> but got <" + actual + ">"); }
  }

  /**
   * Assert expected and actual values are equal within epsilon
   *
   * @param expected Expected value
   * @param actual Actual value
   * @param epsilon Maximum allowed difference between expected and actual
   */
  public static void assertEquals(double expected, double actual, double epsilon) {
    if (Math.abs(actual - expected) > Math.abs(epsilon)) { throw new TCAssertionError("Expected <" + expected
        + "> but got <" + actual + ">"); }
  }

  /**
   * Assert expected and actual values are equal
   *
   * @param expected Expected value
   * @param actual Actual value
   */
  public static void assertEquals(boolean expected, boolean actual) {
    if (expected != actual) { throw new TCAssertionError("Expected <" + expected + "> but got <" + actual + ">"); }
  }

  /**
   * Assert expected and actual values are equal or both null
   *
   * @param expected Expected value
   * @param actual Actual value
   */
  public static void assertEquals(byte[] expected, byte[] actual) {
    boolean expr = (expected == null) ? actual == null : Arrays.equals(expected, actual);
    if (!expr) { throw new TCAssertionError("Got differing byte[]s"); }
  }

  /**
   * Assert expected and actual values are equal or both null
   *
   * @param expected Expected value
   * @param actual Actual value
   */
  public static void assertEquals(Object expected, Object actual) {
    assertEquals(null, expected, actual);
  }

  public static void assertEquals(Object msg, Object expected, Object actual) {
    boolean expr = (expected == null) ? actual == null : expected.equals(actual);
    if (!expr) { throw new TCAssertionError((msg != null ? (msg + ": ") : "") + "Expected <"
                                                           + expected + "> but got <" + actual + ">"); }
  }

  /**
   * Assert that all items in collection are of type elementClass, also if !allowNullElements, check that all items in
   * the collection are non-null.
   *
   * @param collection The collection
   * @param elementClass The expected super type of all items in collection
   * @param allowNullElements Flag for whether null elements are allowed or not
   */
  public static <T> void assertConsistentCollection(Collection<T> collection, Class<T> elementClass, boolean allowNullElements) {
    assertNotNull("Collection", collection);
    assertNotNull("Element class", elementClass);
    for (T element : collection) {
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
   *
   * @param objectArray Array of objects
   * @param requiredElement Must be in objectArray
   */
  public static void assertContainsElement(Object[] objectArray, Object requiredElement) {
    assertNotNull(objectArray);
    for (Object element : objectArray) {
      if (element == requiredElement) return;
    }

    throw failure("Element<" + requiredElement + "> not found in array "
                  + StringUtil.toString(objectArray, ",", "<", ">"));
  }

  public static void assertDoesNotContainsElement(Object[] objectArray, Object element) {
    assertNotNull(objectArray);
    for (Object o : objectArray) {
      if (o == element) {
        failure("Element<" + element + "> was found in array " + StringUtil.toString(objectArray, ",", "<", ">"));
      }
    }
  }

  /**
   * Throw assertion error with generic message
   */
  public static void fail() {
    throw failure("generic failure");
  }

  /**
   * Throw assertion error with specified message
   *
   * @param message Message
   */
  public static void fail(String message) {
    throw failure(message);
  }
}