/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.offheap;

import com.tc.objectserver.storage.api.OffheapStats;

public class OffheapJMXStats implements OffheapStats {
  private final long offheapAllocatedSize;
  private final long offheapFaultObjectCount;
  private final long offheapFlushObjectCount;
  private final long offheapMaxDataSize;
  private final long offheapObjectCachedCount;

  public OffheapJMXStats(final OffheapStats stats) {
    this.offheapAllocatedSize = stats.getOffheapAllocatedDataSize();
    this.offheapFaultObjectCount = stats.getOffHeapFaultRate();
    this.offheapFlushObjectCount = stats.getOffHeapFlushRate();
    this.offheapMaxDataSize = stats.getOffheapMaxDataSize();
    this.offheapObjectCachedCount = stats.getExactOffheapObjectCachedCount();
  }

  public long getOffHeapFaultRate() {
    return offheapFaultObjectCount;
  }

  public long getOffHeapFlushRate() {
    return offheapFlushObjectCount;
  }

  public long getOffheapMaxDataSize() {
    return offheapMaxDataSize;
  }

  public long getOffheapObjectCachedCount() {
    return offheapObjectCachedCount;
  }

  public long getExactOffheapObjectCachedCount() {
    return this.offheapObjectCachedCount;
  }

  public long getOffheapAllocatedDataSize() {
    return this.offheapAllocatedSize;
  }

  @Override
  public String toString() {
    return "OffheapJMXStats [offheapAllocatedSize=" + offheapAllocatedSize + ", offheapFaultObjectCount="
           + offheapFaultObjectCount + ", offheapFlushObjectCount=" + offheapFlushObjectCount + ", offheapMaxDataSize="
           + offheapMaxDataSize + ", offheapObjectCachedCount=" + offheapObjectCachedCount + "]";
  }
}
