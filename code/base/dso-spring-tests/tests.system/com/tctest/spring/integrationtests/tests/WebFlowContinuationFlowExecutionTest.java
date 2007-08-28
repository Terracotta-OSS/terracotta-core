/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import junit.framework.Test;

public class WebFlowContinuationFlowExecutionTest extends WebFlowTestBase {

  public static Test suite() {
    return new WebFlowTestSetup(WebFlowContinuationFlowExecutionTest.class);
  }

  public void testContinuationFlowExecution() throws Exception {
    checkWebFlow("webflow.htm", true);
  }

}
