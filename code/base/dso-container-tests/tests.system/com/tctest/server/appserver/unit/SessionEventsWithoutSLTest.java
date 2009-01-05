/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class SessionEventsWithoutSLTest extends SessionEventsTest {

  public static Test suite() {
    return new SessionEventsWithoutSLSetup();
  }

  private static class SessionEventsWithoutSLSetup extends SessionEventsTestSetup {

    public SessionEventsWithoutSLSetup() {
      super(SessionEventsWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }
}
