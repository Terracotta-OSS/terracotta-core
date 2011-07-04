/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime;

import com.tc.util.runtime.Vm;

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

  private static final String          OLD_GEN_NAME            = "OLD GEN";
  // this pool is used when jdk is run with -client option
  private static final String          TENURED_GEN_NAME        = "TENURED GEN";
  private static final String          IBMJDK_TENURED_GEN_NAME = "Java heap";
  private static final String          JROCKETJDK_OLD_GEN_NAME = "Old Space";

  private final MemoryPoolMXBean       oldGenBean;
  private final GarbageCollectorMXBean oldGenCollectorBean;

  public TCMemoryManagerJdk15PoolMonitor() {
    super();
    this.oldGenBean = getOldGenMemoryPoolBean();
    this.oldGenCollectorBean = getOldGenCollectorBean();
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
        for (String managedPool : managedPools) {
          if (isOldGen(managedPool)) { return gc; }
        }
        gcs2Pools.put(gc.getName(), Arrays.asList(managedPools));
      }
    }
    throw new AssertionError("Old or Tenured Memory pool does not have a garbage collector : " + gcs2Pools);
  }

  private boolean isOldGen(String name) {
    if (Vm.isIBM()) {
      return (name.indexOf(IBMJDK_TENURED_GEN_NAME) > -1);
    } else if (Vm.isJRockit()) {
      return (name.indexOf(JROCKETJDK_OLD_GEN_NAME) > -1);
    } else {
      return (name.toUpperCase().indexOf(OLD_GEN_NAME) > -1 || name.toUpperCase().indexOf(TENURED_GEN_NAME) > -1);
    }
  }

  @Override
  public boolean isMemoryPoolMonitoringSupported() {
    return true;
  }

  @Override
  public MemoryUsage getOldGenUsage() {
    java.lang.management.MemoryUsage oldGenUsage = this.oldGenBean.getUsage();
    return new Jdk15MemoryUsage(oldGenUsage, this.oldGenBean.getName(), this.oldGenCollectorBean.getCollectionCount(),
                                this.oldGenCollectorBean.getCollectionTime());
  }
}
