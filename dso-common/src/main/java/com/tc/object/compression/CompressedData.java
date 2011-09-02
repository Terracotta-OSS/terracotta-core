/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
