/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class CookieDisableWithoutSLTest extends CookieDisableTest {

  public static Test suite() {
    return new CookieDisableWithoutSLSetup();
  }

  public void testSession() throws Exception {
    super.testSession();
  }

  private static class CookieDisableWithoutSLSetup extends CookieDisableTestSetup {

    public CookieDisableWithoutSLSetup() {
      super(CookieDisableWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }
}
