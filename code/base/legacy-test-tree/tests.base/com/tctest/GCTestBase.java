/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.terracottatech.config.PersistenceMode;

public abstract class GCTestBase extends TransparentTestBase implements TestConfigurator {

  int NODE_COUNT                  = 3;
  int LOOP_ITERATION_COUNT        = 1;
  int GARBAGE_COLLECTION_INTERVAL = 10;

  public void setUp() throws Exception {
    super.setUp();
    doSetUp(this);
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(getNodeCount()).setIntensity(LOOP_ITERATION_COUNT);

    TestTVSConfigurationSetupManagerFactory factory = (TestTVSConfigurationSetupManagerFactory) t.getConfigFactory();

    ((SettableConfigItem) factory.l2DSOConfig().garbageCollectionEnabled()).setValue(true);
    ((SettableConfigItem) factory.l2DSOConfig().garbageCollectionVerbose()).setValue(true);
    ((SettableConfigItem) factory.l2DSOConfig().garbageCollectionInterval()).setValue(getGarbageCollectionInterval());
    ((SettableConfigItem) factory.l2DSOConfig().persistenceMode()).setValue(PersistenceMode.TEMPORARY_SWAP_ONLY);

    t.initializeTestRunner();
  }

  protected int getGarbageCollectionInterval() {
    return GARBAGE_COLLECTION_INTERVAL;
  }

  protected int getNodeCount() {
    return NODE_COUNT;
  }

}
