/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.runtime;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

class TCMemoryManagerJdk14 implements JVMMemoryManager {

  private static final TCLogger logger = TCLogging.getLogger(TCMemoryManagerJdk14.class);

  private final Runtime         rt;

  public TCMemoryManagerJdk14() {
    rt = Runtime.getRuntime();
    if (rt.maxMemory() == Long.MAX_VALUE) {
      logger.warn("Please specify Max memory using -Xmx flag for Memory manager to work properly");
    }
  }

  public MemoryUsage getMemoryUsage() {
    return new Jdk14MemoryUsage(rt);
  }

  public MemoryUsage getOldGenUsage() {
    throw new UnsupportedOperationException();
  }

  public boolean isMemoryPoolMonitoringSupported() {
    return false;
  }
  
  private static final class Jdk14MemoryUsage implements MemoryUsage {

    private final long max;
    private final long used;
    private final long free;
    private final long total;
    private final int  usedPercentage;

    public Jdk14MemoryUsage(Runtime rt) {
      this.max = rt.maxMemory();
      this.free = rt.freeMemory();
      this.total = rt.totalMemory();
      this.used = this.total - this.free;
      if (this.max == Long.MAX_VALUE) {
        this.usedPercentage = (int) (this.used * 100 / this.total);
      } else {
        this.usedPercentage = (int) (this.used * 100 / this.max);
      }
    }

    public String getDescription() {
      return "VM 1.4 Memory Usage";
    }

    public long getFreeMemory() {
      return free;
    }

    public long getMaxMemory() {
      return max;
    }

    public long getUsedMemory() {
      return used;
    }
    
    public int getUsedPercentage() {
      return usedPercentage;
    }

    public String toString() {
      return "Jdk14MemoryUsage ( max = " + max + ", used = " + used + ", free = " + free + ", total = " + total
             + ", used % = " + usedPercentage + ")";
    }

    // This is not supported in 1.4
    public long getCollectionCount() {
      return -1;
    }

  }
}
