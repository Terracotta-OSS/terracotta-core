/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
