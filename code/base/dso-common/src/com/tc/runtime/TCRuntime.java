/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.runtime;

import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import java.lang.reflect.Constructor;

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
    if (Vm.isJDK15()) {
      memoryManager = getMemoryManagerJdk15();
    } else {
      memoryManager = new TCMemoryManagerJdk14();
    }
  }

  /*
   * XXX::This method is intensionally written using Reflection so that we dont have any 1.5 dependences.
   * TODO:: Figure a better way to do this.
   */
  private static JVMMemoryManager getMemoryManagerJdk15() {
    try {
      Class c =  Class.forName("com.tc.runtime.TCMemoryManagerJdk15");
      Constructor constructor = c.getConstructor(new Class[0]);
      return (JVMMemoryManager) constructor.newInstance(new Object[0]);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

}
