/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class SimpleJSPWithoutSLTest extends SimpleJSPTest {

  public static Test suite() {
    return new SimpleJSPWithoutSLTestSetup();
  }

  private static class SimpleJSPWithoutSLTestSetup extends SimpleJSPTestSetup {

    public SimpleJSPWithoutSLTestSetup() {
      super(SimpleJSPWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }
}
