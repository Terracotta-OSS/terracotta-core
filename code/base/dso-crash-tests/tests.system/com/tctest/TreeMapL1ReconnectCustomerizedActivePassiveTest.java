/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.Assert;

public class TreeMapL1ReconnectCustomerizedActivePassiveTest extends ActivePassiveTransparentTestBase {

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

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.AP_CUSTOMIZED_CRASH);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.PERMANENT_STORE);
  }

  protected void customizeActivePassiveTest(final ActivePassiveServerManager manager) throws Exception {

    System.out.println("XXX Start active server[0]");
    manager.startServer(0);

    // Allow L1/clients to start, do rest in a thread
    Thread apThread = new Thread(new Runnable() {
      int activeIndex;

      public void run() {
        try {
          Thread.sleep(5000);
          activeIndex = manager.getAndUpdateActiveIndex();
          Assert.assertTrue(activeIndex == 0);
          System.out.println("XXX Stop active server[0]");
          manager.stopServer(0);

          Thread.sleep(100);
          System.out.println("XXX Retart active server[0] for L1 to reconnect");
          manager.startServer(0);
          activeIndex = manager.getAndUpdateActiveIndex();
          Assert.assertTrue(activeIndex == 0);

          System.out.println("XXX Start passive server[1]");
          manager.startServer(1);
          Thread.sleep(1000);
          manager.waitServerIsPassiveStandby(1, 20);

          System.out.println("XXX Stop active server[0] to failover to passive");
          manager.stopServer(0);
          Thread.sleep(2000);
          activeIndex = manager.getAndUpdateActiveIndex();
          Assert.assertTrue(activeIndex == 1);

          manager.cleanupServerDB(0);
          System.out.println("XXX Start passive server[0]");
          manager.startServer(0);
          Thread.sleep(1000);
          manager.waitServerIsPassiveStandby(0, 20);//waitServerIsPassiveStandby(0, 20);
          
          while(true) {
            Thread.sleep(20000);
            int crashedIndex = activeIndex;
            System.out.println("XXX Stop active server[" + activeIndex + "]");
            manager.crashActive();
            
            Thread.sleep(3000);
            manager.cleanupServerDB(crashedIndex);
            System.out.println("XXX Start passive server[" + crashedIndex + "]");
            manager.startServer(crashedIndex);
            Thread.sleep(1000);
            manager.waitServerIsPassiveStandby(crashedIndex, 20);
            
            activeIndex = manager.getAndUpdateActiveIndex();
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
