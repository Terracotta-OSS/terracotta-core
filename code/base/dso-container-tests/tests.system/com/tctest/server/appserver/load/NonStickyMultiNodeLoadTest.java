/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.load;

import com.tc.test.server.appserver.deployment.ServerTestSetup;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

public class NonStickyMultiNodeLoadTest extends MultiNodeLoadTest {

  public static Test suite() {
    // this test has been failing with Websphere and Weblogic 9.2
    // due to health checker zapping L1 while they're in long GC
    // Increase the probes count to be more lienent
    List extraArgs = new ArrayList();
    extraArgs.add("-Dcom.tc.l2.healthCheck.l1.ping.probes=10");
    return new ServerTestSetup(NonStickyMultiNodeLoadTest.class);
  }

  public void testFourNodeLoad() throws Throwable {
    runFourNodeLoad(false);
  }
}
