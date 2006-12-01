/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * JUnit TestCase to exercise the Conversion class.
 * <p>
 * TODO: change tests to use min and max values where appropriate.
 * 
 * @see Conversion
 * @author orion
 */
public class ConversionTest extends TestCase {

  /**
   * Constructor for ConversionTest.
   * 
   * @param arg0
   */
  public ConversionTest(String arg0) {
    super(arg0);
  }

  static final byte[] MAX_UINT_BYTES  = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
  static final byte[] ONE_UINT_BYTES  = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };
  static final byte[] ZERO_UINT_BYTES = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

  public void testBits() {
    byte flags = 0;

    flags = Conversion.setFlag(flags, 1, true);
    flags = Conversion.setFlag(flags, 2, false);
    flags = Conversion.setFlag(flags, 4, true);
    flags = Conversion.setFlag(flags, 8, false);
    flags = Conversion.setFlag(flags, 16, true);

    assertTrue(Conversion.getFlag(flags, 1));
    assertFalse(Conversion.getFlag(flags, 2));
    assertTrue(Conversion.getFlag(flags, 4));
    assertFalse(Conversion.getFlag(flags, 8));
    assertTrue(Conversion.getFlag(flags, 16));

    flags = Conversion.setFlag(flags, 1, false);
    flags = Conversion.setFlag(flags, 2, true);
    flags = Conversion.setFlag(flags, 4, false);
    flags = Conversion.setFlag(flags, 8, true);
    flags = Conversion.setFlag(flags, 16, false);

    assertFalse(Conversion.getFlag(flags, 1));
    assertTrue(Conversion.getFlag(flags, 2));
    assertFalse(Conversion.getFlag(flags, 4));
    assertTrue(Conversion.getFlag(flags, 8));
    assertFalse(Conversion.getFlag(flags, 16));

  }

  public void testuint2Bytes() {
    byte[] bytes = Conversion.uint2bytes(Conversion.MAX_UINT);
    long l = Conversion.bytes2uint(bytes, 0, 4);
    assertTrue(l == Conversion.MAX_UINT);
    assertTrue(Arrays.equals(MAX_UINT_BYTES, bytes));

    bytes = Conversion.uint2bytes(1L);
    l = Conversion.bytes2uint(bytes, 0, 4);
    assertTrue(l == 1L);
    assertTrue(Arrays.equals(ONE_UINT_BYTES, bytes));

    bytes = Conversion.uint2bytes(0L);
    l = Conversion.bytes2uint(bytes, 0, 4);
    assertTrue(l == 0L);
    assertTrue(Arrays.equals(ZERO_UINT_BYTES, bytes));

    bytes = Conversion.uint2bytes(3427655L);
    l = Conversion.bytes2uint(bytes, 0, 4);
    assertTrue(l == 3427655L);
    assertTrue(Arrays.equals(new byte[] { (byte) 0x00, (byte) 0x34, (byte) 0x4D, (byte) 0x47 }, bytes));

    try {
      Conversion.uint2bytes(Conversion.MAX_UINT + 1L);
      assertTrue(false);
    } catch (IllegalArgumentException e) {
      // Exception thrown, we're okay
    }

    try {
      Conversion.uint2bytes(-1L);
      assertTrue(false);
    } catch (IllegalArgumentException e) {
      // Exception thrown, we're okay
    }

  }

  public void testBytes2uint() {
    long result = Conversion.bytes2uint(MAX_UINT_BYTES, 0, 4);
    assertTrue(Conversion.MAX_UINT == result);

    result = Conversion.bytes2uint(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE }, 0, 4);
    assertTrue((Conversion.MAX_UINT - 1L) == result);

    result = Conversion.bytes2uint(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF }, 0, 4);
    assertTrue((Conversion.MAX_UINT - 256L) == result);

    result = Conversion.bytes2uint(new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF }, 0, 4);
    assertTrue((Conversion.MAX_UINT - 65536L) == result);

    result = Conversion.bytes2uint(new byte[] { (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, 0, 4);
    assertTrue((Conversion.MAX_UINT - 16777216L) == result);

    result = Conversion.bytes2uint(ZERO_UINT_BYTES, 0, 4);
    assertTrue(0L == result);

    result = Conversion.bytes2uint(ONE_UINT_BYTES, 0, 4);
    assertTrue(1L == result);

    result = Conversion.bytes2uint(new byte[] { 1, 1, 1, 1 }, 0, 4);
    assertTrue(16843009L == result);

    result = Conversion.bytes2uint(new byte[] { (byte) 0xFF, (byte) 0xFF, 1, (byte) 0xFF, (byte) 0xFF }, 2, 1);
    assertTrue(1L == result);
  }

  public void testByte2uint() {
    int i = 128;

    for (byte b = Byte.MIN_VALUE; b < 0; b++) {
      int result = Conversion.byte2uint(b);
      assertTrue(result == i);
      i++;
    }

    i = 0;
    for (byte b = 0; b < Byte.MAX_VALUE; b++) {
      int result = Conversion.byte2uint(b);
      assertTrue(result == i);
      i++;
    }

  }

  public void testBytes2String() {
    // TODO: add tests for other character sets
    String testString = "test string";
    String convertedString;
    byte[] bytes = Conversion.string2Bytes(testString);
    convertedString = Conversion.bytes2String(bytes);
    assertTrue(testString.equals(convertedString));
  }

  public void testString2Bytes() {
    // testBytes2String();
  }

  public void testBytes2Boolean() {
    boolean testBoolean = true;
    boolean convertedBoolean = false;

    byte[] bytes = Conversion.boolean2Bytes(testBoolean);
    convertedBoolean = Conversion.bytes2Boolean(bytes);
    assertTrue(testBoolean == convertedBoolean);
  }

  public void testBoolean2Bytes() {
    //
  }

  public void testByte2Bytes() {
    byte testByte = 18;
    byte[] convertedBytes = Conversion.byte2Bytes(testByte);
    assertTrue(convertedBytes != null && convertedBytes.length == 1 && convertedBytes[0] == testByte);
  }

  public void testBytes2Char() {
    char testChar = 'c';
    byte[] convertedBytes = Conversion.char2Bytes(testChar);
    assertTrue(testChar == Conversion.bytes2Char(convertedBytes));
  }

  public void testChar2Bytes() {
    //
  }

  public void testBytes2Double() {
    double[] testVals = new double[] { Double.MIN_VALUE, -1.1, 0, 1.1, Double.MAX_VALUE };

    for (int i = 0; i < testVals.length; i++) {
      byte[] convertedBytes = Conversion.double2Bytes(testVals[i]);
      double convertedDouble = Conversion.bytes2Double(convertedBytes);
      assertTrue(testVals[i] == convertedDouble);
    }
  }

  public void testDouble2Bytes() {
    //
  }

  public void testBytes2Float() {
    float[] testVals = new float[] { Float.MIN_VALUE, -1.1f, 0, 1.1f, Float.MAX_VALUE };
    for (int i = 0; i < testVals.length; i++) {
      byte[] convertedBytes = Conversion.float2Bytes(testVals[i]);
      assertTrue(testVals[i] == Conversion.bytes2Float(convertedBytes));
    }
  }

  public void testFloat2Bytes() {
    //
  }

  public void testBytes2Int() {
    int[] testVals = new int[] { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE };
    for (int i = 0; i < testVals.length; i++) {
      try {
        byte[] convertedBytes = Conversion.int2Bytes(testVals[i]);
        int convertedInt = Conversion.bytes2Int(convertedBytes);
        assertEquals(testVals[i], convertedInt);
      } catch (RuntimeException e) {
        e.printStackTrace();
        System.out.println("Failed to convert: " + testVals[i]);
        fail("Failed to convert: " + testVals[i]);
      }
    }
  }

  public void testInt2Bytes() {
    //
  }

  public void testBytes2Long() {
    long[] testVals = new long[] { Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE };
    for (int i = 0; i < testVals.length; i++) {
      byte[] convertedBytes = Conversion.long2Bytes(testVals[i]);
      assertTrue(testVals[i] == Conversion.bytes2Long(convertedBytes));
    }
  }

  public void testLong2Bytes() {
    //
  }

  public void testBytes2Short() {
    short[] testVals = new short[] { Short.MIN_VALUE, -1, 0, 1, Short.MIN_VALUE };
    for (int i = 0; i < testVals.length; i++) {
      byte[] convertedBytes = Conversion.short2Bytes(testVals[i]);
      assertTrue(testVals[i] == Conversion.bytes2Short(convertedBytes));
    }
  }

  public void testShort2Bytes() {
    //
  }

}
