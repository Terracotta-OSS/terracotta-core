/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.test.restart.RestartTestHelper;

public class ConcurrentLockSystemTest extends TransparentTestBase {

  private final int globalParticipantCount = 5;
  private final int intensity              = 1;

  public ConcurrentLockSystemTest() {
    super();
  }

  @Override
  protected void customerizeRestartTestHelper(RestartTestHelper helper) {
    super.customerizeRestartTestHelper(helper);
    helper.getServerCrasherConfig().setRestartInterval(60 * 1000);
  }

  @Override
  protected Class getApplicationClass() {
    return ConcurrentLockSystemTestApp.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(globalParticipantCount).setIntensity(intensity);
    t.initializeTestRunner();
  }

  @Override
  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    configFactory.setGCVerbose(true);
    configFactory.setGCIntervalInSec(10);
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

}
