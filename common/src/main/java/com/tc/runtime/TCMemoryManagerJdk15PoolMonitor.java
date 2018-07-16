/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
import java.util.List;

class TCMemoryManagerJdk15PoolMonitor extends TCMemoryManagerJdk15Basic {

  private static final String          OLD_GEN_NAME            = "OLD GEN";
  // this pool is used when jdk is run with -client option
  private static final String          TENURED_GEN_NAME        = "TENURED GEN";
  private static final String          IBMJDK_TENURED_GEN_NAME = "Java heap";
  private static final String          JROCKETJDK_OLD_GEN_NAME = "Old Space";

  private final boolean memoryPoolMonitoringSupported;
  private final MemoryPoolMXBean       oldGenBean;
  private final GarbageCollectorMXBean oldGenCollectorBean;

  public TCMemoryManagerJdk15PoolMonitor() {
    boolean memoryPoolMonitoringSupportedTmp = false;
    MemoryPoolMXBean oldGenBeanTmp = null;
    GarbageCollectorMXBean oldGenCollectorBeanTmp = null;
    try {
      oldGenBeanTmp = getOldGenMemoryPoolBean();
      oldGenCollectorBeanTmp = getOldGenCollectorBean();
      memoryPoolMonitoringSupportedTmp = true;
    } catch (AssertionError e) {
      // Ignore, will indicate memory monitoring is not supported
    }
    this.oldGenBean = oldGenBeanTmp;
    this.oldGenCollectorBean = oldGenCollectorBeanTmp;
    this.memoryPoolMonitoringSupported = memoryPoolMonitoringSupportedTmp;
  }

  private MemoryPoolMXBean getOldGenMemoryPoolBean() {
    List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
    List<String> poolNames = new ArrayList<String>();
    for (MemoryPoolMXBean mpBean : pools) {
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
    List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
    HashMap<String, List<String>> gcs2Pools = new HashMap<String, List<String>>();
    for (GarbageCollectorMXBean gc : gcs) {
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
    return memoryPoolMonitoringSupported;
  }

  @Override
  public MemoryUsage getOldGenUsage() {
    java.lang.management.MemoryUsage oldGenUsage = this.oldGenBean.getUsage();
    return new Jdk15MemoryUsage(oldGenUsage, this.oldGenBean.getName(), this.oldGenCollectorBean.getCollectionCount(),
                                this.oldGenCollectorBean.getCollectionTime());
  }
}
