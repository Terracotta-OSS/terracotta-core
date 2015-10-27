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
package com.tc.object.dna.impl;

import java.util.Arrays;

public class UTF8ByteCompressedDataHolder extends UTF8ByteDataHolder {
  private static final int HASH_SEED = -1597117597;

  // Used only in case of compressed string
  private final int uncompressedLength;  // of original byte[], not original String
  private final int originalStringLength;
  private final int originalStringHash;

  /**
   * For a possibly interned, compressed string
   */
  public UTF8ByteCompressedDataHolder(byte[] b, int uncompressedLength, int originalStringLength, int originalStringHash) {

    super(b);
    this.uncompressedLength = uncompressedLength;
    this.originalStringLength = originalStringLength;
    this.originalStringHash = originalStringHash;
  }

  private String inflate() {
    return BaseDNAEncodingImpl.inflateCompressedString(getBytes(), uncompressedLength);
  }

  @Override
  public String asString() {
    return inflate();
  }

  @Override
  public int hashCode() {
    return computeHashCode(HASH_SEED);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof UTF8ByteCompressedDataHolder) {
      UTF8ByteCompressedDataHolder other = (UTF8ByteCompressedDataHolder) obj;
      return ((uncompressedLength == other.uncompressedLength) && (Arrays.equals(getBytes(), other.getBytes())));
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
