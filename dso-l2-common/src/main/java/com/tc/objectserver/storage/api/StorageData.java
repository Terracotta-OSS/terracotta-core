/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

public class StorageData {
  private long maxSize;
  private long reservedSize;
  private long usedSize;

  public StorageData() {
    //
  }

  public StorageData(long max, long reserved, long used) {
    this.maxSize = max;
    this.reservedSize = reserved;
    this.usedSize = used;
  }

  public long getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(long maxSize) {
    this.maxSize = maxSize;
  }

  public long getReservedSize() {
    return reservedSize;
  }

  public void setReservedSize(long reservedSize) {
    this.reservedSize = reservedSize;
  }

  public long getUsedSize() {
    return usedSize;
  }

  public void setUsedSize(long usedSize) {
    this.usedSize = usedSize;
  }

}
