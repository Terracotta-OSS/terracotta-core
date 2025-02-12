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

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;

public class TCRuntime {

  private static JVMMemoryManager memoryManager;
  private static GcMonitor gcMonitor;

  static {
    init();
  }

  public static final JVMMemoryManager getJVMMemoryManager() {
    Assert.assertNotNull(memoryManager);
    return memoryManager;
  }

  private static void init() {
    TCProperties props = TCPropertiesImpl.getProperties();

    if (props.getBoolean(TCPropertiesConsts.MEMORY_MONITOR_FORCEBASIC)) {
      memoryManager = new TCMemoryManagerJdk15Basic();
    } else {
      memoryManager = new TCMemoryManagerJdk15PoolMonitor();
    }

    if (props.getBoolean(TCPropertiesConsts.TC_GC_MONITOR_ENABLED)) {
      gcMonitor = new GcMonitor();
      gcMonitor.init();
    }
  }
}
