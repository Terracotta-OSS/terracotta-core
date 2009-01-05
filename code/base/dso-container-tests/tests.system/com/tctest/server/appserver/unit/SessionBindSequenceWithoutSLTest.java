/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class SessionBindSequenceWithoutSLTest extends SessionBindSequenceTest {

  public static Test suite() {
    return new SessionBindSequenceWithoutSLTestSetup();
  }

  private static class SessionBindSequenceWithoutSLTestSetup extends SessionBindSequenceTestSetup {

    public SessionBindSequenceWithoutSLTestSetup() {
      super(SessionBindSequenceWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }

}
