/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance;

import com.tc.test.TCTestCase;

/**
 * The build system needs at least one file with the "Test" postfix in order to prep the testing environment.
 */
public class DummyBuildTest extends TCTestCase {

  public void testFake() {
    assertTrue(true);
  }
}
