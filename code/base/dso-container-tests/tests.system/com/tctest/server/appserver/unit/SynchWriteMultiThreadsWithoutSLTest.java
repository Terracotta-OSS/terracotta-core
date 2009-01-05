/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class SynchWriteMultiThreadsWithoutSLTest extends SynchWriteMultiThreadsTest {

  public static Test suite() {
    return new SynchWriteMultiThreadsWithoutSLTestSetup();
  }

  private static class SynchWriteMultiThreadsWithoutSLTestSetup extends SynchWriteMultiThreadsTestSetup {

    public SynchWriteMultiThreadsWithoutSLTestSetup() {
      super(SynchWriteMultiThreadsWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }

}
