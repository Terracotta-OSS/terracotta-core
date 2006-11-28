/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test;


/**
 * Unit test for {@link TCTestCase}. Simply checks that it can be disabled.
 */
public class TCTestCaseDisablingTest extends TCTestCase {

  public TCTestCaseDisablingTest() {
    disableAllUntil("3000-01-01"); // basically, forever
  }

  public void testFails() throws Exception {
    throw new Exception("This test should never run!");
  }

}