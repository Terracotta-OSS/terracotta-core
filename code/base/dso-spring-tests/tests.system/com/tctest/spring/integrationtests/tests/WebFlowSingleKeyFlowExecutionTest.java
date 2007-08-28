/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import junit.framework.Test;

public class WebFlowSingleKeyFlowExecutionTest extends WebFlowTestBase {

  public static Test suite() {
    return new WebFlowTestSetup(WebFlowSingleKeyFlowExecutionTest.class);
  }

  public void testSingleKeyFlowExecution() throws Exception {
    checkWebFlow("webflow3.htm", false);
  }

}
