/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

public class TCRuntime {

  private static JVMMemoryManager memoryManager;

  static {
    init();
  }

  public static final JVMMemoryManager getJVMMemoryManager() {
    Assert.assertNotNull(memoryManager);
    return memoryManager;
  }

  private static void init() {
    TCProperties props = TCPropertiesImpl.getProperties();

    if (Vm.isJDK15Compliant()) {
      if (props.getBoolean(TCPropertiesConsts.MEMORY_MONITOR_FORCEBASIC)) {
        memoryManager = new TCMemoryManagerJdk15Basic();
      } else {
        memoryManager = new TCMemoryManagerJdk15PoolMonitor();
      }
    } else {
      throw new RuntimeException("JVMMemoryManager require JRE 1.5 or greater");
    }
  }
}
