/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.test.restart.RestartTestHelper;

public class LogicalIteratorTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT = 10;

  public LogicalIteratorTest() {
    //
  }

  @Override
  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    configFactory.l2DSOConfig().getDso().setClientReconnectWindow(30);
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return LogicalIteratorTestApp.class;
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

  @Override
  protected void customizeRestartTestHelper(RestartTestHelper helper) {
    helper.getServerCrasherConfig().setRestartInterval(60 * 1000);
  }
}
