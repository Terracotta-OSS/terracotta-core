/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import junit.framework.Test;

public class JBossSarWithoutSLTest extends JBossSarTest {

  public static Test suite() {
    return new JBossSarWithoutSLTestSetup(JBossSarWithoutSLTest.class, true);
  }
}
