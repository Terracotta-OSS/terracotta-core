/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class SessionInvalidatorWithoutSLTest extends SessionInvalidatorTest {

  public static Test suite() {
    return new SessionInvalidatorWithoutSLTestSetup();
  }

  public void testInvalidator() throws Exception {
    super.testInvalidator();
  }

  private static class SessionInvalidatorWithoutSLTestSetup extends SessionInvalidatorTestSetup {
    public SessionInvalidatorWithoutSLTestSetup() {
      super(SessionInvalidatorWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }
}
