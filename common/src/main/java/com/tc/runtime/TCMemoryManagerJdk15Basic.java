/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

class TCMemoryManagerJdk15Basic implements JVMMemoryManager {

  private static final Logger logger = LoggerFactory.getLogger(TCMemoryManagerJdk15Basic.class);

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
