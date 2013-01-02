/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime;

class Jdk15MemoryUsage implements MemoryUsage {

  private final long   max;
  private final long   free;
  private final long   used;
  private final int    usedPercentage;
  private final String desc;
  private final long   collectionCount;
  private final long   collectionTime;

  public Jdk15MemoryUsage(java.lang.management.MemoryUsage stats, String desc, long collectionCount, long collectionTime) {
    long statsMax = stats.getMax();
    if (statsMax <= 0) {
      this.max = stats.getCommitted();
    } else {
      this.max = statsMax;
    }
    this.used = stats.getUsed();
    this.free = this.max - this.used;
    this.usedPercentage = (int) (this.used * 100 / this.max);
    this.desc = desc;
    this.collectionCount = collectionCount;
    this.collectionTime = collectionTime;
  }

  // CollectionCount is not supported
  public Jdk15MemoryUsage(java.lang.management.MemoryUsage usage, String desc) {
    this(usage, desc, -1, -1);
  }

  @Override
  public String getDescription() {
    return desc;
  }

  @Override
  public long getFreeMemory() {
    return free;
  }

  @Override
  public int getUsedPercentage() {
    return usedPercentage;
  }

  @Override
  public long getMaxMemory() {
    return max;
  }

  @Override
  public long getUsedMemory() {
    return used;
  }

  @Override
  public String toString() {
    return "Jdk15MemoryUsage ( max = " + max + ", used = " + used + ", free = " + free + ", used % = " + usedPercentage
           + ", collectionCount = " + collectionCount + " )";
  }

  @Override
  public long getCollectionCount() {
    return collectionCount;
  }

  @Override
  public long getCollectionTime() {
    return collectionTime;
  }
}
