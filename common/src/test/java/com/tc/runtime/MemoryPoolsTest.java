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

import com.tc.test.TCTestCase;
import com.tc.util.runtime.VmVersion;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import org.junit.Assume;

public class MemoryPoolsTest extends TCTestCase {

  public MemoryPoolsTest() {
    //
  }

  public void testMemoryPools() throws Exception {
    List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
    for (MemoryPoolMXBean mpBean : pools) {
      System.err.println(mpBean);
      System.err.println(" Name = " + mpBean.getName());
      System.err.println(" Usage Threashold supported = " + mpBean.isUsageThresholdSupported());
      System.err.println(" Collection Usage = " + mpBean.getCollectionUsage());
      System.err.println(" Type = " + mpBean.getType());
      System.err.println(" Usage = " + mpBean.getUsage());
      System.err.println("=====================");
    }
    JVMMemoryManager memManager = new TCMemoryManagerJdk15PoolMonitor();
    if (!new VmVersion(System.getProperties()).isIBM()) {
      Assume.assumeTrue(memManager.isMemoryPoolMonitoringSupported());  // false for IBM
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
      /*The assertion is what's bogus about this test. Actual GC is not guaranteed when
      System.gc() is called and the assertion is based on the fallacy that GC will run when
      System.gc() is called.*/
      //assertTrue(collectorCount2 > collectorCount1);
    }
  }
}
