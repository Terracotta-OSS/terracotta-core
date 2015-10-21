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
package com.tc.object.compression;

public class CompressedData {

  private final byte[] data;
  private final int    compressedSize;  // how much of data is really data
  private final int    uncompressedSize;

  public CompressedData(byte[] data, int compressedSize, int uncompressedSize) {
    this.data = data;
    this.compressedSize = compressedSize;
    this.uncompressedSize = uncompressedSize;
  }

  public CompressedData(byte[] data, int uncompressedSize) {
    this.data = data;
    this.compressedSize = data.length;
    this.uncompressedSize = uncompressedSize;
  }

  public byte[] getCompressedData() {
    return data;
  }

  public int getCompressedSize() {
    return this.compressedSize;
  }

  public int getUncompressedSize() {
    return this.uncompressedSize;
  }

}
