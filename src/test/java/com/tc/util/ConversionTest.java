/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;


import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import com.tc.util.Conversion.MemorySizeUnits;
import com.tc.util.Conversion.MetricsFormatException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * JUnit TestCase to exercise the Conversion class.
 * <p>
 * TODO: change tests to use min and max values where appropriate.
 * 
 * @see Conversion
 * @author orion
 */
public class ConversionTest {

  static final byte[] MAX_UINT_BYTES  = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
  static final byte[] ONE_UINT_BYTES  = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };
  static final byte[] ZERO_UINT_BYTES = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
  public void testBytes2String() {
    // TODO: add tests for other character sets
    String testString = "test string";
    String convertedString;
    byte[] bytes = Conversion.string2Bytes(testString);
    convertedString = Conversion.bytes2String(bytes);
    assertTrue(testString.equals(convertedString));
  }

  @Test
  public void testBytes2Boolean() {
    boolean testBoolean = true;
    boolean convertedBoolean = false;

    byte[] bytes = Conversion.boolean2Bytes(testBoolean);
    convertedBoolean = Conversion.bytes2Boolean(bytes);
    assertTrue(testBoolean == convertedBoolean);
  }

  @Test
  public void testByte2Bytes() {
    byte testByte = 18;
    byte[] convertedBytes = Conversion.byte2Bytes(testByte);
    assertTrue(convertedBytes != null && convertedBytes.length == 1 && convertedBytes[0] == testByte);
  }

  @Test
  public void testBytes2Char() {
    char testChar = 'c';
    byte[] convertedBytes = Conversion.char2Bytes(testChar);
    assertTrue(testChar == Conversion.bytes2Char(convertedBytes));
  }

  @Test
  public void testBytes2Double() {
    double[] testVals = new double[] { Double.MIN_VALUE, -1.1, 0, 1.1, Double.MAX_VALUE };

    for (double testVal : testVals) {
      byte[] convertedBytes = Conversion.double2Bytes(testVal);
      double convertedDouble = Conversion.bytes2Double(convertedBytes);
      assertTrue(testVal == convertedDouble);
    }
  }

  @Test
  public void testBytes2Float() {
    float[] testVals = new float[] { Float.MIN_VALUE, -1.1f, 0, 1.1f, Float.MAX_VALUE };
    for (float testVal : testVals) {
      byte[] convertedBytes = Conversion.float2Bytes(testVal);
      assertTrue(testVal == Conversion.bytes2Float(convertedBytes));
    }
  }

  @Test
  public void testBytes2Int() {
    int[] testVals = new int[] { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE };
    for (int testVal : testVals) {
      try {
        byte[] convertedBytes = Conversion.int2Bytes(testVal);
        int convertedInt = Conversion.bytes2Int(convertedBytes);
        assertEquals(testVal, convertedInt);
      } catch (RuntimeException e) {
        throw new AssertionError("Failed to convert: " + testVal + " : " + e);
      }
    }
  }

  @Test
  public void testBytes2Long() {
    long[] testVals = new long[] { Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE };
    for (long testVal : testVals) {
      byte[] convertedBytes = Conversion.long2Bytes(testVal);
      assertTrue(testVal == Conversion.bytes2Long(convertedBytes));
    }
  }

  @Test
  public void testBytes2Short() {
    short[] testVals = new short[] { Short.MIN_VALUE, -1, 0, 1, Short.MIN_VALUE };
    for (short testVal : testVals) {
      byte[] convertedBytes = Conversion.short2Bytes(testVal);
      assertTrue(testVal == Conversion.bytes2Short(convertedBytes));
    }
  }

  @Test
  public void testMemorySizeAsBytes() {
    try {
      Assert.assertEquals(Conversion.memorySizeAsLongBytes("101010"), 101010);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes("101010 "), 101010);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes(" 101010 "), 101010);

      Assert.assertEquals(Conversion.memorySizeAsLongBytes("10m"), 10485760);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes("10 m"), 10485760);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes("10 m "), 10485760);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes("10  m "), 10485760);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes(" 10  m "), 10485760);

      Assert.assertEquals(Conversion.memorySizeAsLongBytes("10g"), 10737418240L);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes("10.1 m"), 10590617);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes("0.75 g "), 805306368);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes("10.5  g "), 11274289152L);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes(" 10.0  g "), 10737418240L);

      Assert.assertEquals(Conversion.memorySizeAsLongBytes(" 10.01   g "), 10748155658L);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes(" 10.01g "), 10748155658L);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes("0.5g"), 536870912);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes(".5g"), 536870912);

      Assert.assertEquals(Conversion.memorySizeAsLongBytes("10.75K"), 11008);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes("0.99G"), 1063004405L);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes("1.0M"), 1048576);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes(".5G"), 536870912);

      Assert.assertEquals(Conversion.memorySizeAsLongBytes("10.75 K"), 11008);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes(" 0.99G "), 1063004405L);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes(" 1.0 M"), 1048576);
      Assert.assertEquals(Conversion.memorySizeAsLongBytes(".50G"), 536870912);

      Assert.assertEquals(Conversion.memorySizeAsLongBytes("100b"), 100);

    } catch (MetricsFormatException mfe) {
      Assert.fail("Not suppose to reach here : " + mfe);
    }

    String[] errStr = { "10 10", " 10 1 ", "10.0ag", " 1 0  m ", "10giga", " 10 mega", "100 100 g", "100.0 ki lo",
        "mega 10 ", "m 10", " k10", "0.75GG", "50M M", "1Kilo" };

    for (String element : errStr) {
      try {
        Conversion.memorySizeAsLongBytes(element);
        Assert.fail("Shouldn't have come here");
      } catch (MetricsFormatException mfe) {
        //expected
      }
    }
  }

  @Test
  public void testMemoryBytesAsSize() {
    char dfs = new DecimalFormatSymbols().getDecimalSeparator();
    Assert.assertEquals("1k", Conversion.memoryBytesAsSize(MemorySizeUnits.KILO.asBytes()));
    Assert.assertEquals("1m", Conversion.memoryBytesAsSize(MemorySizeUnits.MEGA.asBytes()));
    Assert.assertEquals("1g", Conversion.memoryBytesAsSize(MemorySizeUnits.GIGA.asBytes()));

    Assert.assertEquals("4k", Conversion.memoryBytesAsSize(MemorySizeUnits.KILO.asBytes() * 4));
    Assert.assertEquals("8m", Conversion.memoryBytesAsSize(MemorySizeUnits.MEGA.asBytes() * 8));
    Assert.assertEquals("10g", Conversion.memoryBytesAsSize(MemorySizeUnits.GIGA.asBytes() * 10));

    Assert.assertEquals("924b", Conversion.memoryBytesAsSize(MemorySizeUnits.KILO.asBytes() - 100));
    Assert.assertEquals("1024m", Conversion.memoryBytesAsSize(MemorySizeUnits.GIGA.asBytes() - 100));
    Assert.assertEquals("901b", Conversion.memoryBytesAsSize(MemorySizeUnits.KILO.asBytes() - 123));
    Assert.assertEquals("1021" + dfs + "71k", Conversion.memoryBytesAsSize(MemorySizeUnits.MEGA.asBytes() - 2344));
    Assert.assertEquals("933" + dfs + "84m", Conversion.memoryBytesAsSize(MemorySizeUnits.GIGA.asBytes() - 94534540));
  }

  @Test
  public void testMemoryBytesAsJvmSize() throws Exception {
    assertEquals("0b", Conversion.toJvmArgument(0L));
    assertEquals("512b", Conversion.toJvmArgument(512L));

    assertEquals("1k", Conversion.toJvmArgument(MemorySizeUnits.KILO.asBytes()));
    assertEquals("1m", Conversion.toJvmArgument(MemorySizeUnits.MEGA.asBytes()));
    assertEquals("1g", Conversion.toJvmArgument(MemorySizeUnits.GIGA.asBytes()));

    assertEquals("4k", Conversion.toJvmArgument(MemorySizeUnits.KILO.asBytes() * 4));
    assertEquals("5k", Conversion.toJvmArgument(MemorySizeUnits.KILO.asBytes() * 4 + 1));
    assertEquals("4k", Conversion.toJvmArgument(MemorySizeUnits.KILO.asBytes() * 4 - 1));

    assertEquals("8m", Conversion.toJvmArgument(MemorySizeUnits.MEGA.asBytes() * 8));
    assertEquals("9m", Conversion.toJvmArgument(MemorySizeUnits.MEGA.asBytes() * 8 + 1));
    assertEquals("8m", Conversion.toJvmArgument(MemorySizeUnits.MEGA.asBytes() * 8 - 1));

    assertEquals("10g", Conversion.toJvmArgument(MemorySizeUnits.GIGA.asBytes() * 10));
    assertEquals("10241m", Conversion.toJvmArgument(MemorySizeUnits.GIGA.asBytes() * 10 + 1));
    assertEquals("10240m", Conversion.toJvmArgument(MemorySizeUnits.GIGA.asBytes() * 10 - 1));

    assertEquals("924b", Conversion.toJvmArgument(MemorySizeUnits.KILO.asBytes() - 100));
    assertEquals("1024m", Conversion.toJvmArgument(MemorySizeUnits.GIGA.asBytes() - 100));
    assertEquals("901b", Conversion.toJvmArgument(MemorySizeUnits.KILO.asBytes() - 123));
  }
}
