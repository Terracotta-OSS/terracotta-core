/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;

public abstract class GCAndActivePassiveTest extends ActivePassiveTransparentTestBase implements TestConfigurator {

  protected GCConfigurationHelper gcConfigHelper = new GCConfigurationHelper();

  public void setUp() throws Exception {
    super.setUp();
    doSetUp(this);
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(gcConfigHelper.getNodeCount())
        .setIntensity(GCConfigurationHelper.Parameters.LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }

  protected void setupConfig(TestTVSConfigurationSetupManagerFactory configFactory) {
    gcConfigHelper.setupConfig(configFactory);
  }
}
