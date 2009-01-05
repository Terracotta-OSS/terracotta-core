/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.load;

import com.tc.test.server.appserver.deployment.ServerTestSetup;

import junit.framework.Test;

public class MultiNodeInvalidatorWithoutSLTest extends MultiNodeInvalidatorTest {

  public static Test suite() {
    return new ServerTestSetup(MultiNodeInvalidatorWithoutSLTest.class);
  }

  @Override
  public boolean isSessionLockingTrue() {
    return false;
  }

}
