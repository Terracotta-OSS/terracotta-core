/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Holds byte data for strings. The main purpose of this class is to simply hold the bytes data for a String (no sense
 * turning actually into a String instance in L2)
 * <p>
 * The reason it is UTF8ByteDataHolder and not ByteDataHolder is that asString() assumes that the byte data is a valid
 * UTF-8 encoded bytes and creates String as <code> new String(bytes, "UTF-8"); </code>
 */
public class UTF8ByteDataHolder implements Serializable {
  private static final int HASH_SEED = 1704124966;
  private static final int FNV_32_PRIME = 0x01000193;

  private final byte[] bytes;

  // Used for tests
  public UTF8ByteDataHolder(String str) {
    try {
      this.bytes = str.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Just byte data
   */
  public UTF8ByteDataHolder(byte[] b) {
    this.bytes = b;
  }

  public byte[] getBytes() {
    return bytes;
  }

  public String asString() {
    return getString();
  }

  private String getString() {
    try {
      return new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public String toString() {
    return asString();
  }

  @Override
  public int hashCode() {
    return computeHashCode(HASH_SEED);
  }

  protected int computeHashCode(int init) {
    int hash = init;
    for (byte b : bytes) {
      hash ^= b;
      hash *= FNV_32_PRIME;
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof UTF8ByteDataHolder) {
      UTF8ByteDataHolder other = (UTF8ByteDataHolder) obj;
      return (Arrays.equals(this.bytes, other.bytes)) && this.getClass().equals(other.getClass());
    }
    return false;
  }

}
