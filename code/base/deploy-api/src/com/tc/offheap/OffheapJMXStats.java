/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.offheap;

import com.tc.objectserver.storage.api.OffheapStats;

public class OffheapJMXStats implements OffheapStats {
  private final long mapAllocatedSize;
  private final long mapMaxDataSize;
  private final long objectAllocatedSize;
  private final long objectMaxDataSize;
  private final long offheapFaultObjectCount;
  private final long offheapFlushObjectCount;
  private final long offheapMaxDataSize;
  private final long offheapObjectCachedCount;

  public OffheapJMXStats(final OffheapStats stats) {
    this.mapAllocatedSize = stats.getMapAllocatedSize();
    this.mapMaxDataSize = stats.getMapMaxDataSize();
    this.objectAllocatedSize = stats.getObjectAllocatedSize();
    this.objectMaxDataSize = stats.getObjectMaxDataSize();
    this.offheapFaultObjectCount = stats.getOffheapFaultObjectCount();
    this.offheapFlushObjectCount = stats.getOffheapFlushObjectCount();
    this.offheapMaxDataSize = stats.getOffheapMaxDataSize();
    this.offheapObjectCachedCount = stats.getOffheapObjectCachedCount();
  }

  public long getMapAllocatedSize() {
    return mapAllocatedSize;
  }

  public long getMapMaxDataSize() {
    return mapMaxDataSize;
  }

  public long getObjectAllocatedSize() {
    return objectAllocatedSize;
  }

  public long getObjectMaxDataSize() {
    return objectMaxDataSize;
  }

  public long getOffheapFaultObjectCount() {
    return offheapFaultObjectCount;
  }

  public long getOffheapFlushObjectCount() {
    return offheapFlushObjectCount;
  }

  public long getOffheapMaxDataSize() {
    return offheapMaxDataSize;
  }

  public long getOffheapObjectCachedCount() {
    return offheapObjectCachedCount;
  }

  @Override
  public String toString() {
    return "OffheapJMXStats [mapAllocatedSize=" + mapAllocatedSize + ", mapMaxDataSize=" + mapMaxDataSize
           + ", objectAllocatedSize=" + objectAllocatedSize + ", objectMaxDataSize=" + objectMaxDataSize
           + ", offheapFaultObjectCount=" + offheapFaultObjectCount + ", offheapFlushObjectCount="
           + offheapFlushObjectCount + ", offheapMaxDataSize=" + offheapMaxDataSize + ", offheapObjectCachedCount="
           + offheapObjectCachedCount + "]";
  }
}
