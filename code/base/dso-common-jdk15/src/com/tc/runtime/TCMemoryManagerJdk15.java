/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.runtime;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

class TCMemoryManagerJdk15 implements JVMMemoryManager {

  private static final String          OLD_GEN_NAME     = "OLD GEN";
  // this pool is used when jdk is run with -client option
  private static final String          TENURED_GEN_NAME = "TENURED GEN";                                  

  private static final TCLogger        logger           = TCLogging.getLogger(TCMemoryManagerJdk15.class);

  private final MemoryMXBean           memoryBean;

  private final MemoryPoolMXBean       oldGenBean;
  private final GarbageCollectorMXBean oldGenCollectorBean;

  public TCMemoryManagerJdk15() {
    memoryBean = ManagementFactory.getMemoryMXBean();
    java.lang.management.MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    if (heapUsage.getMax() <= 0) {
      logger.warn("Please specify Max memory using -Xmx flag for Memory manager to work properly");
    }
    oldGenBean = getOldGenMemoryPoolBean();
    oldGenCollectorBean = getOldGenCollectorBean();
  }

  private MemoryPoolMXBean getOldGenMemoryPoolBean() {
    List pools = ManagementFactory.getMemoryPoolMXBeans();
    List<String> poolNames = new ArrayList<String>();
    for (Iterator i = pools.iterator(); i.hasNext();) {
      MemoryPoolMXBean mpBean = (MemoryPoolMXBean) i.next();
      String name = mpBean.getName();
      poolNames.add(name);
      if (mpBean.getType() == MemoryType.HEAP && isOldGen(name)) {
        // Got it
        return mpBean;
      }
    }
    throw new AssertionError("Old or Tenured Memory pool Not found : " + poolNames);
  }

  private GarbageCollectorMXBean getOldGenCollectorBean() {
    List gcs = ManagementFactory.getGarbageCollectorMXBeans();
    HashMap<String, List<String>> gcs2Pools = new HashMap<String, List<String>>();
    for (Iterator i = gcs.iterator(); i.hasNext();) {
      GarbageCollectorMXBean gc = (GarbageCollectorMXBean) i.next();
      String[] managedPools = gc.getMemoryPoolNames();
      if (gc.isValid() && managedPools != null) {
        for (int j = 0; j < managedPools.length; j++) {
          if (isOldGen(managedPools[j])) {
            return gc;
          }
        }
        gcs2Pools.put(gc.getName(), Arrays.asList(managedPools));
      }
    }
    throw new AssertionError("Old or Tenured Memory pool does not have a garbage collector : " + gcs2Pools);
  }

  private boolean isOldGen(String name) {
    return (name.toUpperCase().indexOf(OLD_GEN_NAME) > -1 || name.toUpperCase().indexOf(TENURED_GEN_NAME) > -1);
  }

  public boolean isMemoryPoolMonitoringSupported() {
    return true;
  }

  public MemoryUsage getMemoryUsage() {
    java.lang.management.MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    // TODO:: Supporting collectionCount in future. Get a cumulative collectionCount from every collector
    return new Jdk15MemoryUsage(heapUsage, "VM 1.5 Heap Usage");
  }

  public MemoryUsage getOldGenUsage() {
    java.lang.management.MemoryUsage oldGenUsage = oldGenBean.getUsage();
    return new Jdk15MemoryUsage(oldGenUsage, oldGenBean.getName(), oldGenCollectorBean.getCollectionCount());
  }

  private static final class Jdk15MemoryUsage implements MemoryUsage {

    private final long   max;
    private final long   free;
    private final long   used;
    private final int    usedPercentage;
    private final String desc;
    private final long   collectionCount;

    public Jdk15MemoryUsage(java.lang.management.MemoryUsage stats, String desc, long collectionCount) {
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
    }

    // CollectionCount is not supported
    public Jdk15MemoryUsage(java.lang.management.MemoryUsage usage, String desc) {
      this(usage, desc, -1);
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
      return "Jdk15MemoryUsage ( max = " + max + ", used = " + used + ", free = " + free + ", used % = "
             + usedPercentage + ", collectionCount = " + collectionCount +" )";
    }

    public long getCollectionCount() {
      return collectionCount;
    }

  }
}
