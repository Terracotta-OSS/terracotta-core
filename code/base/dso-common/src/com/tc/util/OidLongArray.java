/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

public class OidLongArray {
  public static final int                            BytesPerLong       = 8;
  public static final int                            BitsPerLong        = BytesPerLong * 8;

  private long key;
  private long[] ary;
    
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
    ary = new long[value.length / BytesPerLong];
    for(int i = 0; i < ary.length; ++i) {
      ary[i] = Conversion.bytes2Long(value, i * BytesPerLong);
    }
  }
  
  private long bit(int bitIndex) {
    Assert.assertTrue("Bit index out of range", bitIndex >= 0);
    Assert.assertTrue("Bit index out of range", bitIndex < BitsPerLong);
    return 1L << bitIndex;
  }      

  public void setKey(long key) {
    this.key = key;
  }
  
  public long getKey() {
    return (this.key);
  }
  
  public long[] getArray() {
    return(this.ary);
  }
  
  public byte[] keyToBytes() {
    return Conversion.long2Bytes(key);
  }
  
  public byte[] arrayToBytes() {
    byte[] data = new byte[length() * BytesPerLong];
    for(int i = 0; i < length(); ++i) {
      Conversion.writeLong(ary[i], data, i * BytesPerLong);
    }
    return(data);
  }
  
  public void copyOut(OidLongArray dest, int offset) {
    for(int i = 0; i < dest.length(); ++i) {
      dest.set(i, ary[offset + i]);
    }
  }
  
  public void applyIn(OidLongArray src, int offset) {
    for(int i = 0; i < src.length(); ++i) {
      ary[i+offset] |= src.get(i);
    }
  }
 
  public boolean isZero() {
    for(int i = 0; i < length(); ++i) {
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
    int byteIndex = bit / BitsPerLong;
    int bitIndex = bit % BitsPerLong;
    ary[byteIndex] |= bit(bitIndex);
    return (ary[byteIndex]);
  }
  
  public long clrBit(int bit) {
    int byteIndex = bit / BitsPerLong;
    int bitIndex = bit % BitsPerLong;
    ary[byteIndex] &= ~bit(bitIndex);
    return (ary[byteIndex]);
  }
  
  public boolean isSet(int bit) {
    int byteIndex = bit / BitsPerLong;
    int bitIndex = bit % BitsPerLong;
    return((ary[byteIndex] & bit(bitIndex)) != 0);
  }

}
