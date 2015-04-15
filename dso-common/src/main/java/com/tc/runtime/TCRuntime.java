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
