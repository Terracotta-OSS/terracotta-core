/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class UnlockedSessionObjectWithoutSLTest extends UnlockedSessionObjectTestBase {

  public static Test suite() {
    return new UnlockedSessionObjectWithoutSLTestSetup();
  }

  @Override
  public boolean isSessionLockingTrue() {
    return false;
  }

  private static class UnlockedSessionObjectWithoutSLTestSetup extends UnlockedSessionObjectTestSetup {

    public UnlockedSessionObjectWithoutSLTestSetup() {
      super(UnlockedSessionObjectWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }

}
