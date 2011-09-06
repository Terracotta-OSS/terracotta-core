/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc;

import com.tc.test.TCTestCase;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;

public class ClassLoadInterruptTest extends TCTestCase {
  public void testClassLoadInterrupt() throws Exception {
    new LoadingClass();
    Thread.currentThread().interrupt();
    final Object instance;
    try {
      instance = new NotLoadingClass();
      if (Os.isSolaris() && !Vm.isJDK16Compliant()) {
        fail("Expected NoClassDefFoundError");
      } else {
        new Thread() {
          public void run() {
            System.out.println("Successfullly created " + instance);
          }
        }.start();
      }
    } catch (NoClassDefFoundError e) {
      assertTrue(Os.isSolaris());
      assertTrue(!Vm.isJDK16Compliant());
    }
  }

  static class LoadingClass {
    //
  }

  static class NotLoadingClass {
    //
  }
}
