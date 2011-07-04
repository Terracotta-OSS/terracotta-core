/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  public String getDescription() {
    return desc;
  }

  public long getFreeMemory() {
    return free;
  }

  public int getUsedPercentage() {
    return usedPercentage;
  }

  public long getMaxMemory() {
    return max;
  }

  public long getUsedMemory() {
    return used;
  }

  public String toString() {
    return "Jdk15MemoryUsage ( max = " + max + ", used = " + used + ", free = " + free + ", used % = " + usedPercentage
           + ", collectionCount = " + collectionCount + " )";
  }

  public long getCollectionCount() {
    return collectionCount;
  }

  public long getCollectionTime() {
    return collectionTime;
  }
}