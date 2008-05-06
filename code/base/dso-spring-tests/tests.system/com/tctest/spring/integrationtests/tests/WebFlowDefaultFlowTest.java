/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.ServerTestSetup;

import junit.framework.Test;

public class WebFlowDefaultFlowTest extends WebFlowTestBase {

  public static Test suite() {
    return new ServerTestSetup(WebFlowDefaultFlowTest.class);
  }

  public void testDefaultFlowExecution() throws Exception {
    checkWebFlow("webflow2.htm", false);
  }

}
