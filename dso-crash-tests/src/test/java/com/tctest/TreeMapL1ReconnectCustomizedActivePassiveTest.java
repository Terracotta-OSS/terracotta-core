/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.simulator.app.ErrorContext;
import com.tc.stats.api.DSOMBean;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.test.restart.RestartTestHelper;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;

public class TreeMapL1ReconnectCustomizedActivePassiveTest extends ActivePassiveTransparentTestBase {

  private static final int NODE_COUNT = 2;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return TreeMapTestApp.class;
  }

  @Override
  protected boolean enableL1Reconnect() {
    return true;
  }

  @Override
  protected long getRestartInterval(RestartTestHelper helper) {
    if (Os.isSolaris()) {
      return super.getRestartInterval(helper) * 3;
    } else {
      return super.getRestartInterval(helper);
    }
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

  @Override
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.AP_CUSTOMIZED_CRASH);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.PERMANENT_STORE);
  }

  @Override
  protected void customizeActivePassiveTest(final ActivePassiveServerManager manager) throws Exception {

    System.out.println("XXX Start active server[0]");
    manager.startServer(0);

    // Allow L1/clients to start, do rest in a thread
    Thread apThread = new Thread(new Runnable() {
      int activeIndex;

      public void run() {
        try {

          System.out.println("XXX Waiting for " + NODE_COUNT + "clients to connect");
          while (getClientsForServer(manager, 0) != NODE_COUNT) {
            System.err.println(".");
            ThreadUtil.reallySleep(5000);
          }

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

          System.out.println("XXX Waiting for " + NODE_COUNT + " clients to reconnect");
          while (getClientsForServer(manager, activeIndex) != NODE_COUNT) {
            System.err.println(".");
            ThreadUtil.reallySleep(5000);
          }

          System.out.println("XXX Start passive server[1]");
          manager.startServer(1);
          Thread.sleep(1000);
          Assert.assertTrue("XXX server[1] failed to come up as passive within 5 minutes.",
                            manager.waitServerIsPassiveStandby(1, 300));

          System.out.println("XXX Stop active server[0] to failover to passive");
          manager.stopServer(0);
          Thread.sleep(2000);
          activeIndex = manager.getAndUpdateActiveIndex();
          Assert.assertTrue(activeIndex == 1);

          System.out.println("XXX Waiting for " + NODE_COUNT + " clients to switch over");
          while (getClientsForServer(manager, activeIndex) != NODE_COUNT) {
            System.err.println(".");
            ThreadUtil.reallySleep(5000);
          }

          manager.cleanupServerDB(0);
          System.out.println("XXX Start passive server[0]");
          manager.startServer(0);
          Thread.sleep(1000);
          Assert.assertTrue("XXX server[0] failed to come up as passive within 5 minutes.",
                            manager.waitServerIsPassiveStandby(0, 300));

          while (true) {
            Thread.sleep(20000);
            int crashedIndex = activeIndex;
            System.out.println("XXX Stop active server[" + activeIndex + "]");
            manager.crashActive();

            Thread.sleep(3000);
            manager.cleanupServerDB(crashedIndex);
            System.out.println("XXX Start passive server[" + crashedIndex + "]");
            manager.startServer(crashedIndex);
            Thread.sleep(1000);
            Assert.assertTrue("XXX server[" + crashedIndex + "] failed to come up as passive within 5 minutes.",
                              manager.waitServerIsPassiveStandby(crashedIndex, 300));

            activeIndex = manager.getAndUpdateActiveIndex();
            Assert.assertTrue(activeIndex != crashedIndex);

            System.out.println("XXX Waiting for " + NODE_COUNT + " clients with active");
            while (getClientsForServer(manager, activeIndex) != NODE_COUNT) {
              System.err.println(".");
              ThreadUtil.reallySleep(5000);
            }

          }

        } catch (Throwable t) {
          runner.notifyError(new ErrorContext(t));
        }
      }
    });
    apThread.setDaemon(true);
    apThread.start();
  }

  private int getClientsForServer(ActivePassiveServerManager activePassiveServerManager, int serverIndex)
      throws Exception {
    try {
      DSOMBean mbean = activePassiveServerManager.getDsoMBean(serverIndex);
      for (Object obj : mbean.getClients()) {
        System.out.println("XXX CLIENT BEAN : " + obj);
      }
      return mbean.getClients().length;
    } catch (Exception e) {
      System.err.println("Not able to get DSO Mbean for server index " + serverIndex + " : " + e);
    }
    return 0;
  }
}
