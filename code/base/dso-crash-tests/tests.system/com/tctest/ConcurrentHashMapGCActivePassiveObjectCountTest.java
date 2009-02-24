/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.objectserver.api.GCStats;
import com.tc.stats.DSOMBean;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.util.List;

public class ConcurrentHashMapGCActivePassiveObjectCountTest extends GCAndActivePassiveTest implements TestConfigurator {

  private List<DSOMBean>   dsoMBeans;
  private volatile boolean stopCheckingObjectCount = false;

  public ConcurrentHashMapGCActivePassiveObjectCountTest() {
    // disableAllUntil("2009-03-01");
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setAttribute(ConcurrentHashMapSwappingTestApp.GC_TEST_KEY, "true");
    super.doSetUp(t);
  }

  @Override
  protected Class getApplicationClass() {
    return ConcurrentHashMapSwappingTestApp.class;
  }

  @Override
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH);
    // virtual disable crashing
    setupManager.setServerCrashWaitTimeInSec(40000);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  @Override
  protected void customizeActivePassiveTest(ActivePassiveServerManager manager) throws Exception {
    super.customizeActivePassiveTest(manager);
    if (isMultipleServerTest()) {
      this.dsoMBeans = manager.connectAllDsoMBeans();
      startCheckingServerObjectCount();
    }
  }

  /*
   * This is called when application completed
   */
  @Override
  protected void duringRunningCluster() throws Exception {
    this.stopCheckingObjectCount = true;
    readObjectCount(true);
  }

  public void readObjectCount(boolean doVerify) {
    int totalObjects = 0;
    synchronized (this.dsoMBeans) {
      for (int server = 0; server < this.dsoMBeans.size(); ++server) {
        DSOMBean mbean = this.dsoMBeans.get(server);
        int liveObjects = mbean.getLiveObjectCount();
        GCStats[] stats = mbean.getGarbageCollectorStats();
        System.out.println("XXX Server[" + server + "] live objects = " + liveObjects);
        int gcObjects = 0;
        for (int i = 0; i < stats.length; ++i) {
          System.out.println("XXX Server[" + server + "] stats[" + i + "] garbage " + stats[i].getActualGarbageCount());
          gcObjects += Long.valueOf(stats[i].getActualGarbageCount());
        }
        System.out.println("XXX Server[" + server + "] total objects = " + (liveObjects + gcObjects));
        if (doVerify) {
          if (server == 0) {
            totalObjects = liveObjects + gcObjects;
          } else {
            Assert.assertEquals(totalObjects, liveObjects + gcObjects);
          }
        }
      }
    }
  }

  public void startCheckingServerObjectCount() {
    Thread checker = new Thread(new Runnable() {
      public void run() {
        while (!ConcurrentHashMapGCActivePassiveObjectCountTest.this.stopCheckingObjectCount) {
          ThreadUtil.reallySleep(2000);
          readObjectCount(false);
        }
      }
    });
    checker.start();
  }

}
