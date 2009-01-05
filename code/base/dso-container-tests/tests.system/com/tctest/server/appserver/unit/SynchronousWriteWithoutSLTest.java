/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class SynchronousWriteWithoutSLTest extends SynchronousWriteTest {

  public static Test suite() {
    return new SynchronousWriteWithoutSLTestSetup();
  }

  private static class SynchronousWriteWithoutSLTestSetup extends SynchronousWriteTestSetup {

    public SynchronousWriteWithoutSLTestSetup() {
      super(SynchronousWriteWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }

}
