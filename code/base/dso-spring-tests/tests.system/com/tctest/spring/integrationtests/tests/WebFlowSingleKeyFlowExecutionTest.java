/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.AppServerFactory;

import java.util.Date;

import junit.framework.Test;

public class WebFlowSingleKeyFlowExecutionTest extends WebFlowTestBase {

  public WebFlowSingleKeyFlowExecutionTest() {
    // MNK-377
    if (AppServerFactory.getCurrentAppServerId() == AppServerFactory.WEBLOGIC &&
        AppServerFactory.getCurrentAppServerMajorVersion().equals("9")) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
      
  }
  
  public static Test suite() {
    return new WebFlowTestSetup(WebFlowSingleKeyFlowExecutionTest.class);
  }

  public void testSingleKeyFlowExecution() throws Exception {
    checkWebFlow("webflow3.htm", false);
  }

}
