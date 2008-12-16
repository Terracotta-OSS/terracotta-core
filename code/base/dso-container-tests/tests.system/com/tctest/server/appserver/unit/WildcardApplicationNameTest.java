/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tctest.webapp.servlets.ShutdownNormallyServlet;

import junit.framework.Test;

public class WildcardApplicationNameTest extends SimpleSessionTest {
  private static final String CONFIG_FILE_FOR_TEST = "/tc-config-files/wildcard-tc-config.xml";
  private static final String CONTEXT              = "simple";
  private static final String MAPPING              = "doit";

  public static Test suite() {
    return new WildcardApplicationNameTestSetup();
  }

  /** ****** test setup ********* */
  private static class WildcardApplicationNameTestSetup extends TwoServerTestSetup {

    public WildcardApplicationNameTestSetup() {
      super(WildcardApplicationNameTest.class, CONFIG_FILE_FOR_TEST, CONTEXT);
    }

    @Override
    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("ShutdownNormallyServlet", "/" + MAPPING + "/*", ShutdownNormallyServlet.class, null, false);
    }

  }

}
