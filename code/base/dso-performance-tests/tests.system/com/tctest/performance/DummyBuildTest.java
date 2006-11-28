/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
