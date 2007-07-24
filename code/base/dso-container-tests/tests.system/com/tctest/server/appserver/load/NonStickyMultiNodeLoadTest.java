/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.load;

import com.tc.test.server.appserver.deployment.ServerTestSetup;

import junit.framework.Test;

public class NonStickyMultiNodeLoadTest extends MultiNodeLoadTest {

  public static Test suite() {
    return new ServerTestSetup(NonStickyMultiNodeLoadTest.class);
  }
  
  public void testFourNodeLoad() throws Throwable {
    runFourNodeLoad(false);
  }
}
