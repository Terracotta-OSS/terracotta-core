/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.runtime;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

class TCMemoryManagerJdk15PoolMonitor extends TCMemoryManagerJdk15Basic {

  private static final String          OLD_GEN_NAME     = "OLD GEN";
  // this pool is used when jdk is run with -client option
  private static final String          TENURED_GEN_NAME = "TENURED GEN";                                  

  private final MemoryPoolMXBean       oldGenBean;
  private final GarbageCollectorMXBean oldGenCollectorBean;

  public TCMemoryManagerJdk15PoolMonitor() {
    super();
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

  public MemoryUsage getOldGenUsage() {
    java.lang.management.MemoryUsage oldGenUsage = oldGenBean.getUsage();
    return new Jdk15MemoryUsage(oldGenUsage, oldGenBean.getName(), oldGenCollectorBean.getCollectionCount());
  }
}
