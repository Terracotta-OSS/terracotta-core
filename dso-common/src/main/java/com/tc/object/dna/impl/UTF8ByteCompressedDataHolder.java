/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import java.util.Arrays;

public class UTF8ByteCompressedDataHolder extends UTF8ByteDataHolder {

  // Used only in case of compressed string
  private final int uncompressedLength;  // of original byte[], not original String
  private final int originalStringLength;
  private final int originalStringHash;

  /**
   * For a possibly interned, compressed string
   */
  public UTF8ByteCompressedDataHolder(byte[] b, boolean interned, int uncompressedLength, int originalStringLength,
                                      int originalStringHash) {

    super(b, interned);
    this.uncompressedLength = uncompressedLength;
    this.originalStringLength = originalStringLength;
    this.originalStringHash = originalStringHash;
  }

  private String inflate() {
    return BaseDNAEncodingImpl.inflateCompressedString(getBytes(), uncompressedLength);
  }

  public String asString() {
    return inflate();
  }

  public int hashCode() {
    return computeHashCode(21);
  }
  
  public boolean equals(Object obj) {
    if (obj instanceof UTF8ByteCompressedDataHolder) {
      UTF8ByteCompressedDataHolder other = (UTF8ByteCompressedDataHolder) obj;
      return ((uncompressedLength == other.uncompressedLength) 
          && (Arrays.equals(getBytes(), other.getBytes())));
    }
    return false;
  }

  public boolean isCompressed() {
    return uncompressedLength != -1;
  }

  public int getUncompressedStringLength() {
    return uncompressedLength;
  }

  public int getStringLength() {
    return this.originalStringLength;
  }

  public int getStringHash() {
    return this.originalStringHash;
  }

}
