/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import com.tc.util.runtime.Vm;

import java.util.Date;

/**
 * By inheriting from {@link CorrectJVMTestBase}, checks that the VM that the buildsystem thinks it's running the tests
 * with is the VM that it's *actually* running the tests with.
 * </p>
 * <p>
 * This class should be in a tree declared as using the 1.4 JVM; it's here to make sure the 1.4 JVM is correct.
 */
public class UsingCorrect14JVMTest extends CorrectJVMTestBase {

  public UsingCorrect14JVMTest() {
    if (Vm.isIBM()) {
      // We do not use the 1.4 JDK when running IBM, only 1.5
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  // Nothing here; all test methods are inherited.

}
