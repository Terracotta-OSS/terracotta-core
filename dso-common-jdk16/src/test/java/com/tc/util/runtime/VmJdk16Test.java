/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import junit.framework.TestCase;

/**
 * Test to see if this class is valid for JRE 1-6 environment
 */
public class VmJdk16Test extends TestCase {

  public void testValidateJdk16() {
    if (Vm.isJDK16()) {
      assertFalse(Vm.isJDK14());
      assertFalse(Vm.isJDK15());
      assertTrue(Vm.isJDK15Compliant());
      assertTrue(Vm.isJDK16());
      assertTrue(Vm.isJDK16Compliant());
      assertFalse(Vm.isJDK17());
    }
  }

}
