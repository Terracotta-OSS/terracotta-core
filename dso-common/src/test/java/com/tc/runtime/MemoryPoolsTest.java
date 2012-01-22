/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime;

import com.tc.test.TCTestCase;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.Iterator;
import java.util.List;

public class MemoryPoolsTest extends TCTestCase {

  public MemoryPoolsTest() {
    //
  }

  public void testMemoryPools() throws Exception {
    List pools = ManagementFactory.getMemoryPoolMXBeans();
    for (Iterator i = pools.iterator(); i.hasNext();) {
      MemoryPoolMXBean mpBean = (MemoryPoolMXBean) i.next();
      System.err.println(mpBean);
      System.err.println(" Name = " + mpBean.getName());
      System.err.println(" Usage Threashold supported = " + mpBean.isUsageThresholdSupported());
      System.err.println(" Collection Usage = " + mpBean.getCollectionUsage());
      System.err.println(" Type = " + mpBean.getType());
      System.err.println(" Usage = " + mpBean.getUsage());
      System.err.println("=====================");
    }
    JVMMemoryManager memManager = new TCMemoryManagerJdk15PoolMonitor();
    assertTrue(memManager.isMemoryPoolMonitoringSupported());
    MemoryUsage mu1 = memManager.getOldGenUsage();
    assertNotNull(mu1);
    long collectorCount1 = mu1.getCollectionCount();
    System.err.println("Collector Count  = " + collectorCount1);
    assertTrue(collectorCount1 > -1);
    System.gc();
    MemoryUsage mu2 = memManager.getOldGenUsage();
    assertNotNull(mu2);
    long collectorCount2 = mu2.getCollectionCount();
    System.err.println("Now the Collector Count  is  " + collectorCount2);
    assertTrue(collectorCount2 > collectorCount1);
  }
}
