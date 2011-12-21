/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.test.TCTestCase;

public class TestQuarantineTestExpectedToFailTest extends TCTestCase {

  public void testShouldFail() {
    throw new AssertionError("This is a temporary test to make sure a NEW is executed by the quarantine monkey and the failure email is sent correctly");
  }

}
