/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class NewSessionAfterInvalidateWithoutSLTest extends NewSessionAfterInvalidateTest {

  public static Test suite() {
    return new NewSessionAfterInvalidateWithoutSLSetup();
  }

  private static class NewSessionAfterInvalidateWithoutSLSetup extends NewSessionTestSetup {

    public NewSessionAfterInvalidateWithoutSLSetup() {
      super(NewSessionAfterInvalidateWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }

}
