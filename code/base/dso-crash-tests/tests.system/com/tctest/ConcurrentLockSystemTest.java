/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;

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
    TestTVSConfigurationSetupManagerFactory factory = (TestTVSConfigurationSetupManagerFactory) t.getConfigFactory();

    ((SettableConfigItem) factory.l2DSOConfig().garbageCollectionVerbose()).setValue(true);
    ((SettableConfigItem) factory.l2DSOConfig().garbageCollectionInterval()).setValue(10);

    t.getTransparentAppConfig().setClientCount(globalParticipantCount).setIntensity(intensity);
    t.initializeTestRunner();
  }

  protected boolean canRunCrash() {
    return true;
  }

}
