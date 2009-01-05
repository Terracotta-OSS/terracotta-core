/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class SessionIdFromURLWithoutSLTest extends SessionIDFromURLTest {

  public static Test suite() {
    return new SessionIdFromURLWithoutSLTestSetup();
  }

  private static class SessionIdFromURLWithoutSLTestSetup extends SessionIDFromURLTestSetup {

    public SessionIdFromURLWithoutSLTestSetup() {
      super(SessionIdFromURLWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }
}
