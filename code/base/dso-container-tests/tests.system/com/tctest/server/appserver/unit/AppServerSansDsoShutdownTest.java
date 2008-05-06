/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

/**
 * Test to make sure the app server shutdown normally with DSO
 */
public class AppServerSansDsoShutdownTest extends AppServerShutdownTestBase {

  public AppServerSansDsoShutdownTest() {
    super(false);
  }

  public static Test suite() {
    return new AppServerShutdownTestSetup(AppServerSansDsoShutdownTest.class, false);
  }

}
