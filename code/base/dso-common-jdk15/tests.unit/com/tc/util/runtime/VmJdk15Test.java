/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import junit.framework.TestCase;

/**
 * Test to see if this class is valid for JRE 1-5 environment
 */
public class VmJdk15Test extends TestCase {

  public void testValidateJdk15() {
    if (Vm.isJDK15()) {
      assertFalse(Vm.isJDK14());
      assertTrue(Vm.isJDK15());
      assertTrue(Vm.isJDK15Compliant());
      assertFalse(Vm.isJDK16());
      assertFalse(Vm.isJDK16Compliant());
      assertFalse(Vm.isJDK17());
    }
  }

}
