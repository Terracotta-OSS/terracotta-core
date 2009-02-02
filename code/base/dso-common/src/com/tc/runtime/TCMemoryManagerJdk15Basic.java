/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.runtime;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

class TCMemoryManagerJdk15Basic implements JVMMemoryManager {

  private static final TCLogger        logger           = TCLogging.getLogger(TCMemoryManagerJdk15Basic.class);

  private final MemoryMXBean           memoryBean;

  public TCMemoryManagerJdk15Basic() {
    memoryBean = ManagementFactory.getMemoryMXBean();
    java.lang.management.MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    if (heapUsage.getMax() <= 0) {
      logger.warn("Please specify Max memory using -Xmx flag for Memory manager to work properly");
    }
  }
  
  public boolean isMemoryPoolMonitoringSupported() {
    return false;
  }

  public MemoryUsage getMemoryUsage() {
    java.lang.management.MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    // TODO:: Supporting collectionCount in future. Get a cumulative collectionCount from every collector
    return new Jdk15MemoryUsage(heapUsage, "VM 1.5 Heap Usage");
  }

  public MemoryUsage getOldGenUsage() {
    throw new UnsupportedOperationException();
  }
}
