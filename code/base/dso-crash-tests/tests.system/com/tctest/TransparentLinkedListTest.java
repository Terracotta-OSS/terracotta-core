/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.terracottatech.configV2.PersistenceMode;

public class TransparentLinkedListTest extends TransparentTestBase implements TestConfigurator {
  private static final int NODE_COUNT           = 3;
  private static final int EXECUTION_COUNT      = 3;
  private static final int LOOP_ITERATION_COUNT = 3;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATION_COUNT);
    TestTVSConfigurationSetupManagerFactory factory = (TestTVSConfigurationSetupManagerFactory) t.getConfigFactory();

    ((SettableConfigItem) factory.l2DSOConfig().garbageCollectionEnabled()).setValue(true);
    ((SettableConfigItem) factory.l2DSOConfig().persistenceMode()).setValue(PersistenceMode.TEMPORARY_SWAP_ONLY);

    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return TransparentLinkedListTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

}
