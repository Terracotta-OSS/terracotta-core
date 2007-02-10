/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.longrunning;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.terracottatech.configV2.PersistenceMode;

public class LongrunningGCTester extends TransparentTestBase implements TestConfigurator {

  int NODE_COUNT           = 3;
  int LOOP_ITERATION_COUNT = 1;

  public void setUp() throws Exception {
    super.setUp();
    doSetUp(this);
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(LOOP_ITERATION_COUNT);

    TestTVSConfigurationSetupManagerFactory factory = (TestTVSConfigurationSetupManagerFactory) t.getConfigFactory();

    ((SettableConfigItem) factory.l2DSOConfig().garbageCollectionEnabled()).setValue(true);
    ((SettableConfigItem) factory.l2DSOConfig().garbageCollectionVerbose()).setValue(true);
    ((SettableConfigItem) factory.l2DSOConfig().persistenceMode()).setValue(PersistenceMode.TEMPORARY_SWAP_ONLY);

    t.getRunnerConfig().executionTimeout = Long.MAX_VALUE;
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LongrunningGCTestApp.class;
  }

}
