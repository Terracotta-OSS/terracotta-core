/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

/**
 * Test to make sure the app server shutdown normally *without* DSO -- The point of this test is to make sure the
 * framework (and the container itself) can be started/stopped reliably even if DSO isn't in the mix
 */
public class AppServerShutdownTest extends AppServerShutdownTestBase {

  public AppServerShutdownTest() {
    super(true);
  }

  public static Test suite() {
    return new AppServerShutdownTestSetup(AppServerShutdownTest.class, true);
  }

}
