/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class SessionEventsWithTCPropsWithoutSLTest extends SessionEventsWithTCPropsTest {

  public static Test suite() {
    return new SessionEventsWithTCPropsWithoutSLTestSetup();
  }

  private static class SessionEventsWithTCPropsWithoutSLTestSetup extends SessionEventsWithTCPropsTestSetup {

    public SessionEventsWithTCPropsWithoutSLTestSetup() {
      super(SessionEventsWithTCPropsWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }

}
