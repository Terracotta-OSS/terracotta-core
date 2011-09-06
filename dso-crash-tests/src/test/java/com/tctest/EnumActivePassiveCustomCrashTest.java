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

public class EnumActivePassiveCustomCrashTest extends ActivePassiveTransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Class getApplicationClass() {
    return EnumTestApp.class;
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

  @Override
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(3);
    setupManager.setServerCrashMode(MultipleServersCrashMode.AP_CUSTOMIZED_CRASH);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
    setupManager.setElectionTime(15);
  }

  @Override
  protected void customizeActivePassiveTest(final ActivePassiveServerManager manager) throws Exception {

    System.out.println("XXX Starting first server");
    manager.startServer(0);

    // Allow L1/clients to start, do rest in a thread
    Thread apThread = new Thread(new Runnable() {
      int   activeIndex;
      int[] passiveIndex = new int[2];

      public void run() {
        try {
          int count = 0;

          while (count < 3) {
            Thread.sleep(5000);
            activeIndex = manager.getAndUpdateActiveIndex();
            System.err.println("XXX Active is index " + activeIndex);

            Thread.sleep(30000);

            System.err.println("XXX Start 2 passives");
            int p = 0;
            for (int i = 0; i < 3; i++) {
              if (i == activeIndex) continue;
              manager.startServer(i);
              passiveIndex[p++] = i;
            }

            Thread.sleep(5000);
            manager.waitServerIsPassiveStandby(passiveIndex[0], 60);
            System.err.println("XXX Passive 1 index : " + passiveIndex[0]);
            manager.waitServerIsPassiveStandby(passiveIndex[1], 60);
            System.err.println("XXX Passive 2 index : " + passiveIndex[1]);

            System.err.println("XXX Stop Active " + activeIndex);
            manager.stopServer(activeIndex);
            activeIndex = manager.getAndUpdateActiveIndex();

            System.err.println("XXX Stop one passive " + passiveIndex[0]);
            manager.stopServer(passiveIndex[0]);

            Thread.sleep(5000);
            count++;
          }

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    apThread.setDaemon(true);
    apThread.start();
  }
}