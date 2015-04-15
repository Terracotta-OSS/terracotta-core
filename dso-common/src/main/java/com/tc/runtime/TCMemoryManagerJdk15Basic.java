/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
  
  @Override
  public boolean isMemoryPoolMonitoringSupported() {
    return false;
  }

  @Override
  public MemoryUsage getMemoryUsage() {
    java.lang.management.MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    // TODO:: Supporting collectionCount in future. Get a cumulative collectionCount from every collector
    return new Jdk15MemoryUsage(heapUsage, "VM 1.5 Heap Usage");
  }

  @Override
  public MemoryUsage getOldGenUsage() {
    throw new UnsupportedOperationException();
  }
}
