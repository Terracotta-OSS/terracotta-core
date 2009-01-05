/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class ResponseIsCommittedWithoutSLTest extends ResponseIsCommittedTest {

  public static Test suite() {
    return new ResponseIsCommittedWithoutSLSetup();
  }

  private static class ResponseIsCommittedWithoutSLSetup extends ResponseIsCommittedTestTestSetup {

    public ResponseIsCommittedWithoutSLSetup() {
      super(ResponseIsCommittedWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }
}
