/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.objectserver;

import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassivePersistenceMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tctest.YoungGCTestBase;



public class CreateRescueCandidatesYoungGCTest extends YoungGCTestBase {

 
  public CreateRescueCandidatesYoungGCTest() {
   //
  }
  
  protected boolean canRunActivePassive() {
    return true;
  }
  
  // start only 1 L1
  protected int getNodeCount() {
    return 4;
  }
  
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(60);
    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  protected Class getApplicationClass() {
    return CreateRescueCandidatesYoungGCTestApp.class;
  }

  protected int getGarbageCollectionInterval() {
    return 180;
  }
  
  
}
