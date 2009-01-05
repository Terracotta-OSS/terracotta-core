/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class CookieRenameWithoutSLTest extends CookieRenameTest {

  public static Test suite() {
    return new CookieRenameWithoutSLSetup();
  }

  public void test() throws Exception {
    super.test();
  }

  private static class CookieRenameWithoutSLSetup extends CookieRenameTestSetup {
    public CookieRenameWithoutSLSetup() {
      super(CookieRenameWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }
}
