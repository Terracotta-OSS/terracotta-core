/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class SimpleSessionFilterWithoutSLTest extends SimpleSessionFilterTest {

  public static Test suite() {
    return new SimpleSessionFilterWithoutSLTestSetup();
  }

  private static class SimpleSessionFilterWithoutSLTestSetup extends SimpleSessionFilterTestSetup {

    public SimpleSessionFilterWithoutSLTestSetup() {
      super(SimpleSessionFilterWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }

}
