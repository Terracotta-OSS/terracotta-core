/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassivePersistenceMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.Assert;

public class TreeMapL1ReconnectCustomerizedActivePassiveTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return TreeMapTestApp.class;
  }
  
  protected boolean enableL1Reconnect() {
    return true;
  }

  protected boolean canRunCrash() {
    return true;
  }

  protected boolean canRunActivePassive() {
    return true;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveCrashMode.AP_CUSTOMERIZED_CRASH);
    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.PERMANENT_STORE);
  }

  protected void customerizeActivePassiveTest() throws Exception {

    System.out.println("XXX Start active server[0]");
    apStartServer(0);

    // Allow L1/clients to start, do rest in a thread
    Thread apThread = new Thread(new Runnable() {
      int activeIndex;

      public void run() {
        try {
          Thread.sleep(5000);
          activeIndex = apGetActiveIndex();
          Assert.assertTrue(activeIndex == 0);
          System.out.println("XXX Stop active server[0]");
          apStopServer(0);

          Thread.sleep(100);
          System.out.println("XXX Retart active server[0] for L1 to reconnect");
          apStartServer(0);
          activeIndex = apGetActiveIndex();
          Assert.assertTrue(activeIndex == 0);

          System.out.println("XXX Start passive server[1]");
          apStartServer(1);
          Thread.sleep(1000);
          waitServerIsPassiveStandby(1, 20);

          System.out.println("XXX Stop active server[0] to failover to passive");
          apStopServer(0);
          Thread.sleep(2000);
          activeIndex = apGetActiveIndex();
          Assert.assertTrue(activeIndex == 1);

          apCleanupServerDB(0);
          System.out.println("XXX Start passive server[0]");
          apStartServer(0);
          Thread.sleep(1000);
          waitServerIsPassiveStandby(0, 20);
          
          while(true) {
            Thread.sleep(20000);
            int crashedIndex = activeIndex;
            System.out.println("XXX Stop active server[" + activeIndex + "]");
            apCrashActiveserver();
            
            Thread.sleep(3000);
            apCleanupServerDB(crashedIndex);
            System.out.println("XXX Start passive server[" + crashedIndex + "]");
            apStartServer(crashedIndex);
            Thread.sleep(1000);
            waitServerIsPassiveStandby(crashedIndex, 20);
            
            activeIndex = apGetActiveIndex();
            Assert.assertTrue(activeIndex != crashedIndex);
          }

        } catch (Exception e) {
          System.out.println("customerizeActivePassiveTest: " + e);
        }
      }
    });
    apThread.setDaemon(true);
    apThread.start();
  }

}
