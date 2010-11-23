/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;

public class ConcurrentLockSystemTest extends TransparentTestBase {

  private final int globalParticipantCount = 5;
  private final int intensity              = 1;

  public ConcurrentLockSystemTest() {
    super();
  }

  protected Class getApplicationClass() {
    return ConcurrentLockSystemTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(globalParticipantCount).setIntensity(intensity);
    t.initializeTestRunner();
  }

  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    configFactory.setGCVerbose(true);
    configFactory.setGCIntervalInSec(10);
  }

  protected boolean canRunCrash() {
    return true;
  }

}
