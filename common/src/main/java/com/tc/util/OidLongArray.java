/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

public class OidLongArray {
  public static final int BYTES_PER_LONG = 8;
  public static final int BITS_PER_LONG  = BYTES_PER_LONG * 8;

  private long            key;
  private long[]          ary;

  public OidLongArray(int size, long key) {
    ary = new long[size];
    this.key = key;
  }

  public OidLongArray(byte[] key, byte[] value) {
    // ary null is used as an indicator for end of processing
    if (key == null || value == null) {
      ary = null;
      return;
    }

    this.key = Conversion.bytes2Long(key);
    ary = valueToArray(value);
  }
  
  public OidLongArray(long key, byte[] value) {
    if (value == null) {
      ary = null;
      return;
    }

    this.key = key;
    ary = valueToArray(value);
  }
  
  private long[] valueToArray(byte[] value) {
    long[] lary = new long[value.length / BYTES_PER_LONG];
    for (int i = 0; i < lary.length; ++i) {
      lary[i] = Conversion.bytes2Long(value, i * BYTES_PER_LONG);
    }
    return lary;
  }

  private long bit(int bitIndex) {
    Assert.assertTrue("Bit index out of range", bitIndex >= 0);
    Assert.assertTrue("Bit index out of range", bitIndex < BITS_PER_LONG);
    return 1L << bitIndex;
  }

  public void setKey(long key) {
    this.key = key;
  }

  public long getKey() {
    return (this.key);
  }

  public long[] getArray() {
    return (this.ary);
  }

  public byte[] keyToBytes() {
    return keyToBytes(0);
  }

  public byte[] keyToBytes(int auxKey) {
    return Conversion.long2Bytes(key + auxKey);
  }

  public byte[] arrayToBytes() {
    byte[] data = new byte[length() * BYTES_PER_LONG];
    for (int i = 0; i < length(); ++i) {
      Conversion.writeLong(ary[i], data, i * BYTES_PER_LONG);
    }
    return (data);
  }

  public void copyOut(OidLongArray dest, int offset) {
    for (int i = 0; i < dest.length(); ++i) {
      dest.set(i, ary[offset + i]);
    }
  }

  public void applyIn(OidLongArray src, int offset) {
    for (int i = 0; i < src.length(); ++i) {
      ary[i + offset] |= src.get(i);
    }
  }

  public boolean isZero() {
    for (int i = 0; i < length(); ++i) {
      if (ary[i] != 0) return (false);
    }
    return (true);
  }

  // use null array as an indicator of end of record
  public boolean isEnded() {
    return (ary == null);
  }

  public long get(int index) {
    return ary[index];
  }

  public void set(int index, long val) {
    ary[index] = val;
  }

  public int length() {
    return ary.length;
  }

  public long setBit(int bit) {
    int byteIndex = bit / BITS_PER_LONG;
    int bitIndex = bit % BITS_PER_LONG;
    ary[byteIndex] |= bit(bitIndex);
    return (ary[byteIndex]);
  }

  public long clrBit(int bit) {
    int byteIndex = bit / BITS_PER_LONG;
    int bitIndex = bit % BITS_PER_LONG;
    ary[byteIndex] &= ~bit(bitIndex);
    return (ary[byteIndex]);
  }

  public boolean isSet(int bit) {
    int byteIndex = bit / BITS_PER_LONG;
    int bitIndex = bit % BITS_PER_LONG;
    return ((ary[byteIndex] & bit(bitIndex)) != 0);
  }

  public int totalBits() {
    return ary.length * BITS_PER_LONG;
  }

}
