/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class SimpleSessionWithoutSLTest extends SimpleSessionTest {
  public static final String CONFIG_FILE_NO_SL_FOR_TEST = "/tc-config-files/simplesession-no-sl-tc-config.xml";

  public static Test suite() {
    return new SimpleSessionWithoutSLTestSetup();
  }

  private static class SimpleSessionWithoutSLTestSetup extends SimpleSessionTestSetup {

    public SimpleSessionWithoutSLTestSetup() {
      super(SimpleSessionWithoutSLTest.class, CONFIG_FILE_NO_SL_FOR_TEST, CONTEXT);
    }

  }

}
