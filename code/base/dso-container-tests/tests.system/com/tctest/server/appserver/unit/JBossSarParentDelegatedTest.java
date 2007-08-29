/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.tc.test.server.appserver.AppServerFactory;
import com.tc.util.runtime.Vm;

import java.util.Date;

import junit.framework.Test;

public class JBossSarParentDelegatedTest extends JBossSarTest {

  public JBossSarParentDelegatedTest() {
    if (AppServerFactory.getCurrentAppServerId() != AppServerFactory.JBOSS || Vm.isJDK14()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
    parentDelegation = true;
  }

  public void testSar() throws Exception {    
    doTest();
  }

  public static Test suite() {
    return new JBossSarTestSetup(JBossSarParentDelegatedTest.class);
  }
}
