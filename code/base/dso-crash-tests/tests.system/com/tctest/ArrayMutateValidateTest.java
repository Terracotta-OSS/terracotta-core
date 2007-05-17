/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassivePersistenceMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tctest.runner.TransparentAppConfig;

public class ArrayMutateValidateTest extends TransparentTestBase {

  public static final int      MUTATOR_NODE_COUNT      = 2;
  public static final int      VALIDATOR_NODE_COUNT    = 1;
  public static final int      APP_INSTANCE_PER_NODE   = 2;
  private static final boolean IS_MUTATE_VALIDATE_TEST = true;

  public void doSetUp(TransparentTestIface t) throws Exception {
    TransparentAppConfig tac = t.getTransparentAppConfig();
    tac.setClientCount(MUTATOR_NODE_COUNT).setIntensity(1).setValidatorCount(VALIDATOR_NODE_COUNT)
        .setApplicationInstancePerClientCount(APP_INSTANCE_PER_NODE);
    t.initializeTestRunner(IS_MUTATE_VALIDATE_TEST);
  }

  protected Class getApplicationClass() {
    return MutateValidateArrayTestApp.class;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveCrashMode.CRASH_AFTER_MUTATE);
    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.TEMPORARY_SWAP_ONLY);
    
//    setupManager.setServerCount(3);
//    setupManager.setServerCrashMode(ActivePassiveCrashMode.RANDOM_SERVER_CRASH);
//    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.NETWORK);
//    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.TEMPORARY_SWAP_ONLY);
//    setupManager.setServerCrashWaitTimeInSec(15);
  }

  protected boolean canRunActivePassive() {
    return true;
  }
}
