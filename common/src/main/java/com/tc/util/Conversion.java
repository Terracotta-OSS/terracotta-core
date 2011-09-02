/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.TCException;
import com.tc.exception.TCRuntimeException;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.regex.Pattern;

/**
 * Data conversion algorithms and whatnot can be found in java.io.DataInput and java.io.DataOutput. Contains methods for
 * converting from one kind of thing to another.
 * 
 * @author orion
 */
public class Conversion {

  public static final long MIN_UINT = 0;
  public static final long MAX_UINT = 4294967295L; // 2^32 - 1

  private static int makeInt(byte b3, byte b2, byte b1, byte b0) {
    return ((((b3 & 0xff) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | ((b0 & 0xff) << 0)));
  }

  public static byte setFlag(byte flags, int offset, boolean value) {
    if (value) {
      return (byte) (flags | offset);
    } else {
      return (byte) (flags & ~offset);
    }
  }

  public static boolean getFlag(byte flags, int offset) {
    return (flags & offset) == offset;
  }

  /**
   * Helper method to convert a byte to an unsigned int. Use me when you want to treat a java byte as unsigned
   */
  public static short byte2uint(byte b) {
    return (short) (b & 0xFF);
  }

  public static long bytes2uint(byte b[]) {
    return bytes2uint(b, 0, b.length);
  }

  public static long bytes2uint(byte b[], int length) {
    return bytes2uint(b, 0, length);
  }

  /**
   * Helper method to convert 1-4 bytes (big-endiand) into an unsigned int (max: 2^32 -1)
   */
  public static long bytes2uint(byte b[], int offset, int length) {
    if ((length < 1) || (length > 4)) { throw new IllegalArgumentException("invalid byte array length: " + length); }

    if ((b.length - offset) < length) { throw new IllegalArgumentException("not enough data available for length "
                                                                           + length + " starting at offset " + offset
                                                                           + " in a byte array of length " + b.length); }

    long rv = 0;

    switch (length) {
      case 1: {
        return byte2uint(b[offset]);
      }
      case 2: {
        rv += ((long) byte2uint(b[0 + offset])) << 8;
        rv += byte2uint(b[1 + offset]);
        return rv;
      }
      case 3: {
        rv += ((long) byte2uint(b[0 + offset])) << 16;
        rv += ((long) byte2uint(b[1 + offset])) << 8;
        rv += byte2uint(b[2 + offset]);
        return rv;
      }
      case 4: {
        rv += ((long) byte2uint(b[0 + offset])) << 24;
        rv += ((long) byte2uint(b[1 + offset])) << 16;
        rv += ((long) byte2uint(b[2 + offset])) << 8;
        rv += byte2uint(b[3 + offset]);
        return rv;
      }
      default: {
        throw new RuntimeException("internal error");
      }
    }
  }

  /**
   * Helper method to write a 4 byte unsigned integer value into a given byte array at a given offset
   * 
   * @param l the unsigned int value to write
   * @param dest the byte array to write the uint into
   * @param index starting offset into the destination byte array
   */
  public static void writeUint(long l, byte[] dest, int index) {
    if ((l > MAX_UINT) || (l < 0)) { throw new IllegalArgumentException("unsigned integer value invalid: " + l); }

    int pos = index;

    dest[pos++] = (byte) ((l >>> 24) & 0x000000FF);
    dest[pos++] = (byte) ((l >>> 16) & 0x000000FF);
    dest[pos++] = (byte) ((l >>> 8) & 0x000000FF);
    dest[pos++] = (byte) (l & 0x000000FF);

    return;
  }

  /**
   * Helper method to write a 4 byte java (signed) integer value into a given byte array at a given offset
   * 
   * @param l the signed int value to write
   * @param dest the byte array to write the uint into
   * @param index starting offset into the destination byte array
   */
  public static void writeInt(int i, byte[] dest, int index) {
    dest[index] = (byte) ((i >>> 24) & 0x000000FF);
    dest[index + 1] = (byte) ((i >>> 16) & 0x000000FF);
    dest[index + 2] = (byte) ((i >>> 8) & 0x000000FF);
    dest[index + 3] = (byte) ((i >>> 0) & 0x000000FF);

    return;
  }

  /**
   * Helper method to convert an unsigned short to 2 bytes (big-endian)
   */
  public static byte[] ushort2bytes(int i) {
    if ((i > 0xFFFF) || (i < 0)) { throw new IllegalArgumentException("invalid short value: " + i); }

    byte[] rv = new byte[2];

    rv[0] = (byte) ((i >>> 8) & 0x000000FF);
    rv[1] = (byte) ((i >>> 0) & 0x000000FF);

    return rv;
  }

  /**
   * Helper method to convert an unsigned integer to 4 bytes (big-endian)
   */
  public static byte[] uint2bytes(long l) {
    if ((l > MAX_UINT) || (l < 0)) { throw new IllegalArgumentException("unsigned integer value out of range: " + l); }

    byte[] rv = new byte[4];

    rv[0] = (byte) ((l >>> 24) & 0x000000FF);
    rv[1] = (byte) ((l >>> 16) & 0x000000FF);
    rv[2] = (byte) ((l >>> 8) & 0x000000FF);
    rv[3] = (byte) ((l >>> 0) & 0x000000FF);

    return rv;
  }

  /**
   * Helper method to convert a string to bytes in a safe way.
   */
  public static String bytes2String(byte[] bytes) {
    try {
      return new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new TCRuntimeException(e);
    }
  }

  /**
   * Helper method to convert a string to bytes in a safe way.
   */
  public static byte[] string2Bytes(String string) {
    try {
      return (string == null) ? new byte[0] : string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new TCRuntimeException(e);
    }
  }

  public static boolean bytes2Boolean(byte[] bytes) {
    return (bytes[0] != 0 ? true : false);
  }

  public static byte[] boolean2Bytes(boolean v) {
    byte[] rv = new byte[1];

    rv[0] = v ? (byte) 1 : (byte) 0;
    return rv;
  }

  public static byte[] byte2Bytes(byte v) {
    return new byte[] { v };
  }

  public static char bytes2Char(byte[] bytes) {
    return (char) ((bytes[0] << 8) | (bytes[1] & 0xff));
  }

  public static byte[] char2Bytes(char v) {
    return new byte[] { (byte) (0xff & (v >> 8)), (byte) (0xff & v) };
  }

  public static double bytes2Double(byte[] bytes) {
    return Double.longBitsToDouble(Conversion.bytes2Long(bytes));
  }

  public static byte[] double2Bytes(double l) {
    return Conversion.long2Bytes(Double.doubleToLongBits(l));
  }

  public static float bytes2Float(byte[] bytes) {
    return Float.intBitsToFloat(Conversion.bytes2Int(bytes));
  }

  public static byte[] float2Bytes(float l) {
    return Conversion.int2Bytes(Float.floatToIntBits(l));
  }

  public static int bytes2Int(byte[] bytes, int offset) {
    return makeInt(bytes[offset], bytes[offset + 1], bytes[offset + 2], bytes[offset + 3]);
  }

  public static int bytes2Int(byte[] bytes) {
    return makeInt(bytes[0], bytes[1], bytes[2], bytes[3]);
  }

  public static byte[] int2Bytes(int v) {
    byte[] rv = new byte[4];

    rv[0] = (byte) ((v >>> 24) & 0xFF);
    rv[1] = (byte) ((v >>> 16) & 0xFF);
    rv[2] = (byte) ((v >>> 8) & 0xFF);
    rv[3] = (byte) ((v >>> 0) & 0xFF);

    return rv;
  }

  public static long bytes2Long(byte[] bytes) {
    return bytes2Long(bytes, 0);
  }

  public static long bytes2Long(byte[] bytes, int offset) {

    return (((long) bytes[offset] << 56) + ((long) (bytes[offset + 1] & 0xFF) << 48)
            + ((long) (bytes[offset + 2] & 0xFF) << 40) + ((long) (bytes[offset + 3] & 0xFF) << 32)
            + ((long) (bytes[offset + 4] & 0xFF) << 24) + ((bytes[offset + 5] & 0xFF) << 16)
            + ((bytes[offset + 6] & 0xFF) << 8) + ((bytes[offset + 7] & 0xFF) << 0));
  }

  public static byte[] long2Bytes(long l) {
    byte[] rv = new byte[8];
    writeLong(l, rv, 0);
    return rv;
  }

  public static void writeLong(long l, byte[] rv, int offset) {

    rv[offset] = (byte) (l >>> 56);
    rv[offset + 1] = (byte) (l >>> 48);
    rv[offset + 2] = (byte) (l >>> 40);
    rv[offset + 3] = (byte) (l >>> 32);
    rv[offset + 4] = (byte) (l >>> 24);
    rv[offset + 5] = (byte) (l >>> 16);
    rv[offset + 6] = (byte) (l >>> 8);
    rv[offset + 7] = (byte) (l >>> 0);

  }

  public static short bytes2Short(byte[] bytes) {
    short rv = (short) (((bytes[0] & 0xFF) << 8) + ((bytes[1] & 0xFF) << 0));
    return rv;
  }

  public static byte[] short2Bytes(short v) {
    byte[] rv = new byte[2];

    rv[0] = (byte) ((v >>> 8) & 0xFF);
    rv[1] = (byte) ((v >>> 0) & 0xFF);

    return rv;
  }

  public static boolean byte2Boolean(byte b) {
    return b != 0;
  }

  /**
   * @param value
   */
  public static byte boolean2Byte(boolean value) {
    return (value) ? (byte) 1 : (byte) 0;
  }

  /**
   * Equivalent to calling <code>bytesToHex(b, 0, b.length)</code>.
   */
  public static String bytesToHex(byte[] b) {
    return bytesToHex(b, 0, b.length);
  }

  /**
   * Converts a single byte to a hex string representation, can be decoded with Byte.parseByte().
   * 
   * @param b the byte to encode
   * @return a
   */
  public static String bytesToHex(byte[] b, int index, int length) {
    StringBuffer buf = new StringBuffer();
    byte leading, trailing;
    for (int pos = index; pos >= 0 && pos < index + length && pos < b.length; ++pos) {
      leading = (byte) ((b[pos] >>> 4) & 0x0f);
      trailing = (byte) (b[pos] & 0x0f);
      buf.append((0 <= leading) && (leading <= 9) ? (char) ('0' + leading) : (char) ('A' + (leading - 10)));
      buf.append((0 <= trailing) && (trailing <= 9) ? (char) ('0' + trailing) : (char) ('A' + (trailing - 10)));
    }
    return buf.toString();
  }

  /**
   * @param hexString
   * @return an array of bytes, decoded from the hex-encoded string, if <code>hexString</code> is <code>null</code> or
   *         the length is not a multiple of two then <code>null</code> is returned.
   */
  public static byte[] hexToBytes(String hexString) {
    if (hexString == null || hexString.length() % 2 != 0) { return null; }
    int length = hexString.length();
    byte rv[] = new byte[length / 2];
    int x, y;
    for (x = 0, y = 0; x < length; x += 2, ++y) {
      rv[y] = Byte.parseByte(hexString.substring(x, x + 2), 16);
    }
    return rv;
  }

  public static String buffer2String(int length, TCByteBuffer buffer) {
    byte[] bytes = new byte[length];
    buffer.get(bytes);
    return Conversion.bytes2String(bytes);
  }

  public static enum MemorySizeUnits {
    KILO("k"), MEGA("m"), GIGA("g");

    private final String unit;

    private MemorySizeUnits(final String unit) {
      this.unit = unit;
    }

    public long getInBytes() throws MetricsFormatException {
      if (this.unit.equals(KILO.getUnit())) {
        return 1024;
      } else if (this.unit.equals(MEGA.getUnit())) {
        return 1024 * KILO.getInBytes();
      } else if (this.unit.equals(GIGA.getUnit())) {
        return 1024 * MEGA.getInBytes();
      } else {
        throw new MetricsFormatException("Unexpected size: " + toString());
      }
    }

    @Override
    public String toString() {
      return "'" + this.unit + "'";
    }

    public String getUnit() {
      return unit;
    }

  }

  // XXX: FIX rename method
  public static int memorySizeAsIntBytes(final String memorySizeInUnits) throws MetricsFormatException {
    long rv = memorySizeAsLongBytes(memorySizeInUnits);
    if (rv > Integer.MAX_VALUE) {
      throw new MetricsFormatException(memorySizeInUnits + " is greater than integer range");
    } else {
      return (int) rv;
    }
  }

  public static long memorySizeAsLongBytes(final String memorySizeInUnits) throws MetricsFormatException {
    final String input = memorySizeInUnits.toLowerCase().trim();
    // XXX: review pattern matcher regex
    if (!Pattern.matches("[0-9]*([.][0-9]+)? *([kmg])?", input)) { throw new MetricsFormatException("Unexpected Size: "
                                                                                                    + input); }
    String[] str = input.split("[kmg]");
    if (str.length > 1) { throw new MetricsFormatException("Unexpected size: " + input); }

    double base = 0;
    try {
      base = Double.parseDouble(str[0].trim());
    } catch (NumberFormatException nfe) {
      throw new MetricsFormatException("Unexpectes metrics: " + input);
    }

    if (input.endsWith(MemorySizeUnits.KILO.getUnit())) {
      return (long) (base * MemorySizeUnits.KILO.getInBytes());
    } else if (input.endsWith(MemorySizeUnits.MEGA.getUnit())) {
      return (long) (base * MemorySizeUnits.MEGA.getInBytes());
    } else if (input.endsWith(MemorySizeUnits.GIGA.getUnit())) {
      return (long) (base * MemorySizeUnits.GIGA.getInBytes());
    } else {
      return (long) base;
    }
  }

  static DecimalFormat twoDForm = new DecimalFormat();
  static {
    twoDForm.applyLocalizedPattern("#" + new DecimalFormatSymbols().getDecimalSeparator() + "##");
  }

  public static String memoryBytesAsSize(final long bytes) throws NumberFormatException, MetricsFormatException {
    if (bytes < MemorySizeUnits.KILO.getInBytes()) {
      return bytes + "b";
    } else if (bytes < MemorySizeUnits.MEGA.getInBytes()) {
      double rv = (bytes / (MemorySizeUnits.KILO.getInBytes() * 1.0));
      return twoDForm.format(rv) + "k";
    } else if (bytes < MemorySizeUnits.GIGA.getInBytes()) {
      double rv = (bytes / (MemorySizeUnits.MEGA.getInBytes() * 1.0));
      return twoDForm.format(rv) + "m";
    } else {
      double rv = (bytes / (MemorySizeUnits.GIGA.getInBytes() * 1.0));
      return twoDForm.format(rv) + "g";
    }
  }

  public static class MetricsFormatException extends TCException {
    public MetricsFormatException(String message) {
      super(message);
    }
  }

}