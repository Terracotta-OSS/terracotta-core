/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

/**
 * Unit test for {@link StandardStringifier}.
 */
public class StandardStringifierTest extends TCTestCase {

  private StandardStringifier stringifier;

  public void setUp() {
    this.stringifier = StandardStringifier.INSTANCE;
  }

  public void testNull() throws Exception {
    assertEquals("<null>", stringifier.toString(null));
  }

  private static class MyObj {
    private final String value;

    public MyObj(String value) {
      this.value = value;
    }

    public String toString() {
      return this.value;
    }
  }

  public void testObject() throws Exception {
    assertEquals("FOOBAR", stringifier.toString(new MyObj("FOOBAR")));
  }

  public void testObjectArray() throws Exception {
    assertEquals("Object[3]: FOOBAR, barbaz, FOOFLE", stringifier.toString(new Object[] { new MyObj("FOOBAR"),
        new MyObj("barbaz"), new MyObj("FOOFLE") }));
    assertEquals("Object[22]: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, ...", stringifier
        .toString(new Object[] { new MyObj("1"), new MyObj("2"), new MyObj("3"), new MyObj("4"), new MyObj("5"),
            new MyObj("6"), new MyObj("7"), new MyObj("8"), new MyObj("9"), new MyObj("10"), new MyObj("11"),
            new MyObj("12"), new MyObj("13"), new MyObj("14"), new MyObj("15"), new MyObj("16"), new MyObj("17"),
            new MyObj("18"), new MyObj("19"), new MyObj("20"), new MyObj("21"), new MyObj("22") }));
  }

  public void testOurObjectArray() throws Exception {
    assertEquals("StandardStringifierTest.MyObj[3]: FOOBAR, barbaz, FOOFLE", stringifier.toString(new MyObj[] {
        new MyObj("FOOBAR"), new MyObj("barbaz"), new MyObj("FOOFLE") }));
    assertEquals(
                 "StandardStringifierTest.MyObj[22]: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, ...",
                 stringifier.toString(new MyObj[] { new MyObj("1"), new MyObj("2"), new MyObj("3"), new MyObj("4"),
                     new MyObj("5"), new MyObj("6"), new MyObj("7"), new MyObj("8"), new MyObj("9"), new MyObj("10"),
                     new MyObj("11"), new MyObj("12"), new MyObj("13"), new MyObj("14"), new MyObj("15"),
                     new MyObj("16"), new MyObj("17"), new MyObj("18"), new MyObj("19"), new MyObj("20"),
                     new MyObj("21"), new MyObj("22") }));
  }
  
  public void testByteArray() throws Exception {
    assertEquals("3 bytes: 0102 03  ...", stringifier.toString(new byte[] { 1, 2, 3 }));
  }

  public void testShortArray() throws Exception {
    assertEquals("short[3]: 1, 2, 3", stringifier.toString(new short[] { 1, 2, 3 }));
    assertEquals("short[22]: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, ...", stringifier
        .toString(new short[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22 }));
  }

  public void testIntArray() throws Exception {
    assertEquals("int[3]: 1, 2, 3", stringifier.toString(new int[] { 1, 2, 3 }));
    assertEquals("int[22]: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, ...", stringifier
        .toString(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22 }));
  }

  public void testLongArray() throws Exception {
    assertEquals("long[3]: 1, 2, 3", stringifier.toString(new long[] { 1, 2, 3 }));
    assertEquals("long[22]: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, ...", stringifier
        .toString(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22 }));
  }

  public void testCharArray() throws Exception {
    assertEquals("char[3]: 1, 2, 3", stringifier.toString(new char[] { '1', '2', '3' }));
    assertEquals("char[22]: 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, f, g, h, i, j, k, ...", stringifier
        .toString(new char[] { '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'l', 'm' }));
  }

  public void testBooleanArray() throws Exception {
    assertEquals("boolean[3]: true, false, true", stringifier.toString(new boolean[] { true, false, true }));
    assertEquals(
                 "boolean[22]: true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, ...",
                 stringifier.toString(new boolean[] { true, false, true, false, true, false, true, false, true, false,
                     true, false, true, false, true, false, true, false, true, false, true, false }));
  }

  public void testFloatArray() throws Exception {
    assertEquals("float[3]: 1.0, 2.0, 3.0", stringifier.toString(new float[] { 1.0f, 2.0f, 3.0f }));
    assertEquals("float[22]: 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, ...", stringifier
        .toString(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 14.0f, 15.0f, 16.0f, 17.0f, 18.0f, 19.0f, 20.0f, 21.0f, 22.0f }));
  }

  public void testDoubleArray() throws Exception {
    assertEquals("double[3]: 1.0, 2.0, 3.0", stringifier.toString(new double[] { 1.0, 2.0, 3.0 }));
    assertEquals("double[22]: 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, ...", stringifier
        .toString(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0 }));
  }
 
}