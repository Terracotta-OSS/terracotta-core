/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import junit.framework.TestCase;

public class SystemMemoryTest extends TestCase {

  public void testGetTotalSystemMemory() throws Exception {
    if (Os.isSolaris()) {
      validateMemoryValue();
      return;
    }

    try {
      SystemMemory.getTotalSystemMemory();
      fail("should have thrown an exception");
    } catch (UnsupportedOperationException uoe) {
      // expected
    }
  }

  private void validateMemoryValue() {
    final long memory = SystemMemory.getTotalSystemMemory();
    System.err.println("memory value is " + memory);

    if (memory <= 0) {
      fail("invalid memory value: " + memory);
    }

    final long vmMax = Runtime.getRuntime().maxMemory();
    if (vmMax > memory) {
      fail("VM max memory (" + vmMax + ") exceeds system memory (" + memory + ")");
    }
  }
}
