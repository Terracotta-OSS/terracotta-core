/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;

public class ObjectDataRapidL2DisconnectActivePassiveTest extends ActivePassiveTransparentTestBase implements
    TestConfigurator {

  private final int clientCount = 1;

  @Override
  protected Class getApplicationClass() {
    return ObjectDataRapidL2DisconnectActivePassiveTestApp.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
  }

  @Override
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.AP_CUSTOMIZED_CRASH);

    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  @Override
  protected void setExtraJvmArgs(ArrayList jvmArgs) {
    super.setExtraJvmArgs(jvmArgs);
    jvmArgs.add("-Dcom.tc.seda." + ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE + ".sleepMs=10");
  }

  @Override
  protected void customizeActivePassiveTest(final ActivePassiveServerManager manager) throws Exception {
    System.out.println("XXXXXX starting custmized test");
    System.out.println("XXX Start active server[0]");
    manager.startServer(0);
    manager.getAndUpdateActiveIndex();
    Thread.sleep(5000);

    Thread th = new Thread(new Runnable() {

      public void run() {
        System.out.println("XXX Start passive server[1]");
        try {
          manager.startServer(1);
          Thread.sleep(1000);

          while (true) {
            ThreadUtil.reallySleep(5 * 1000);
            manager.stopServer(1);
            manager.startServer(1);
          }

        } catch (Exception e) {
          e.printStackTrace();
        }

      }
    });
    th.setDaemon(true);
    th.start();
  }

}
