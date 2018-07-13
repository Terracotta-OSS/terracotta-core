/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.test;

import org.junit.jupiter.api.Assertions;

import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * TCAssertions
 */
public class TCAssertions {

  public static void assertContainsIgnoreCase(String expected, String actual) {
    assertContainsIgnoreCase(null, expected, actual);
  }

  public static void assertContainsIgnoreCase(String message, String expected, String actual) {
    assertContains(message, expected != null ? expected.toLowerCase() : null, actual != null ? actual.toLowerCase()
        : null);
  }

  public static void assertContains(String expected, String actual) {
    assertContains(null, expected, actual);
  }

  public static void assertEqualsUnordered(Object[] a1, Object[] a2) {
    if (a1.length != a2.length) {
      throw new AssertionError(Arrays.asList(a1) + " != " + Arrays.asList(a2));
    }

    List<Object> asList = Arrays.asList(a2);

    for (Object o : a1) {
      if (! asList.contains(o)) {
        throw new AssertionError(Arrays.asList(a1) + " != " + Arrays.asList(a2));
      }
    }
  }

  public static void assertContains(String message, String expected, String actual) {
    if ((expected == null) != (actual == null)) {
      message = (message == null ? "" : message + ": ");
      Assertions.fail(message + "Expected was " + (expected == null ? "<null>" : "'" + expected + "'") + ", but actual was "
                      + (actual == null ? "<null>" : "'" + actual + "'"));
    }

    if (expected != null) {
      if (actual.indexOf(expected) < 0) {
        message = (message == null ? "" : message + ": ");
        Assertions.fail(message + "Actual string '" + actual + "' does not contain expected string '" + expected + "'");
      }
    }
  }

  public static void fail(Throwable t) {
    fail("FAILURE", t);
  }

  public static void fail(String message, Throwable t) {
    AssertionError err = new AssertionError(message == null ? "" : (message + "\n"));
    err.initCause(t);
    throw err;
  }

  public static void assertNotEquals(int i1, int i2) {
    Assertions.assertFalse(i1 == i2, () -> "Values are equal: " + i1);
  }

  public static void assertEquals(byte[] b1, byte[] b2) {
    boolean rv = (b1 == null) ? b2 == null : Arrays.equals(b1, b2);
    Assertions.assertTrue(rv, () -> "Values are not equals");
  }

  public static void assertNotEquals(Object o1, Object o2) {
    Assertions.assertFalse(o1 == o2, () -> "Values are equal: " + o1 + ", " + o2 );
    if (o1 != null && o2 != null) {
      Assertions.assertFalse(o1.equals(o2), () -> "Values are equal: " + o1 + ", " + o2);
      Assertions.assertFalse(o2.equals(o1), () -> "Values are equal: " + o1 + ", " + o2);
    }
  }

  public static void assertSerializable(Object obj) {
    assertSerializable(obj, true, true);
  }

  public static void assertSerializable(Object obj, boolean checkEquals, boolean checkHashCode) {
    Assertions.assertNotNull(obj);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos;
    Object deserializedObj = null;
    try {
      oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      oos.close();
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
      deserializedObj = ois.readObject();
    } catch (IOException ioe) {
      throw Assert.failure("Object failed to serialize", ioe);
    } catch (ClassNotFoundException cnfe) {
      throw Assert.failure("Object failed to serialize", cnfe);
    }
    Assertions.assertNotNull(obj);
    if (checkEquals) {
      Assertions.assertEquals(obj, deserializedObj, () -> "Object and [de]serialized object failed equals() comparison");
    }
    if (checkHashCode) {
      Assertions.assertEquals(obj.hashCode(),
          deserializedObj.hashCode(), "Object and [de]serialized object failed hashCode() comparison");
    }
  }

  public static void checkComparator(Object smaller, Object bigger, Object equalToBigger, Comparator<Object> c) {
    // test null's
    Assertions.assertTrue(c.compare(null, bigger) < 0);
    Assertions.assertTrue(c.compare(bigger, null) > 0);
    Assertions.assertTrue(c.compare(null, null) == 0);

    // test less-than
    Assertions.assertTrue(c.compare(smaller, bigger) < 0);

    // test greater-than
    Assertions.assertTrue(c.compare(bigger, smaller) > 0);

    // test equal
    Assertions.assertTrue(c.compare(bigger, equalToBigger) == 0);
    Assertions.assertTrue(c.compare(equalToBigger, bigger) == 0);
  }


  /**
   *   a way to ensure that system clock moves forward...
   *
   * @param previousSystemMillis
   * @return current time in milliseconds
   */
  public static synchronized long assertTimeDirection(final long previousSystemMillis) {
    long currentMillis = System.currentTimeMillis();
    Assertions.assertTrue(currentMillis >= previousSystemMillis, () -> "System Clock Moved Backwards! [current=" + currentMillis + ", previous=" + previousSystemMillis + "]");

    return currentMillis;
  }
}
