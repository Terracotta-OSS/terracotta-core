/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

  private final byte[] bytes;
  private int          hash = 0;

  // Used for tests
  public UTF8ByteDataHolder(String str) {
    try {
      this.bytes = str.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  public UTF8ByteDataHolder(byte[] b) {
    this.bytes = b;
  }

  public byte[] getBytes() {
    return bytes;
  }

  public String asString() {
    try {
      return new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  public String toString() {
    return asString();
  }

  public int hashCode() {
    if (hash == 0) {
      int tmp = 0;
      for (int i = 0, n = bytes.length; i < n; i++) {
        tmp = 31 * tmp + bytes[i++];
      }
      hash = tmp;
    }
    return hash;
  }

  public boolean equals(Object obj) {
    if (obj instanceof UTF8ByteDataHolder) {
      UTF8ByteDataHolder other = (UTF8ByteDataHolder) obj;
      return ((Arrays.equals(this.bytes, other.bytes)) && this.getClass().equals(other.getClass()));
    }
    return false;
  }
}
