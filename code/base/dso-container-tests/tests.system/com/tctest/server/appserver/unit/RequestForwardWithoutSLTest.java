/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class RequestForwardWithoutSLTest extends RequestForwardTest {

  public static Test suite() {
    return new RequestForwardWithoutSLSetup();
  }

  private static class RequestForwardWithoutSLSetup extends RequestForwardTestSetup {

    public RequestForwardWithoutSLSetup() {
      super(RequestForwardWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }
}
