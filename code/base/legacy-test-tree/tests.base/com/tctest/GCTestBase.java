/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.terracottatech.config.PersistenceMode;

public abstract class GCTestBase extends TransparentTestBase implements TestConfigurator {

  private final int     NODE_COUNT                  = 3;
  private final int     LOOP_ITERATION_COUNT        = 1;
  private final int     GARBAGE_COLLECTION_INTERVAL = 10;
  private final boolean GC_ENABLED                  = true;
  private final boolean GC_VERBOSE                  = true;

  public void setUp() throws Exception {
    super.setUp();
    doSetUp(this);
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(getNodeCount()).setIntensity(LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }

  protected void setupConfig(TestTVSConfigurationSetupManagerFactory configFactory) {
    configFactory.setGCEnabled(getGCEnabled());
    configFactory.setGCVerbose(getGCVerbose());
    configFactory.setGCIntervalInSec(getGarbageCollectionInterval());
    configFactory.setPersistenceMode(PersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  protected boolean getGCEnabled() {
    return GC_ENABLED;
  }

  protected boolean getGCVerbose() {
    return GC_VERBOSE;
  }

  protected int getGarbageCollectionInterval() {
    return GARBAGE_COLLECTION_INTERVAL;
  }

  protected int getNodeCount() {
    return NODE_COUNT;
  }

}
