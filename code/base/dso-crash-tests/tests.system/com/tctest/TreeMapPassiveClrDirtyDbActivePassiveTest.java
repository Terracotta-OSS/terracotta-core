/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.cli.command.BaseCommand;
import com.tc.cli.command.SetDbCleanCommand;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.Assert;

import java.io.File;
import java.io.OutputStreamWriter;

/*
 * DEV-2011. For the case, both active and passive go down. But active goes down for good. Passive restores data and
 * becomes active.
 */
public class TreeMapPassiveClrDirtyDbActivePassiveTest extends ActivePassiveTransparentTestBase {

  private static final int NODE_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return TreeMapTestApp.class;
  }
  
//  protected boolean enableL1Reconnect() {
//    return true;
//  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.AP_CUSTOMIZED_CRASH);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.DISK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.PERMANENT_STORE);
  }

  protected void customizeActivePassiveTest(final ActivePassiveServerManager manager) throws Exception {

    System.out.println("XXX Start active server[0]");
    manager.startServer(0);
    manager.getAndUpdateActiveIndex();
    manager.getAndUpdateActiveIndex();
    Thread.sleep(5000);
    
    System.out.println("XXX Start passive server[1]");
    manager.startServer(1);
    Thread.sleep(1000);
    manager.waitServerIsPassiveStandby(1, 20);
    
    // Allow L1/clients to start, do rest in a thread
    Thread apThread = new Thread(new Runnable() {

      public void run() {
        int activeIndex;
        
        try {
          // to run a while
          Thread.sleep(10000);

          System.out.println("XXX Stop passive server[1]");
          manager.stopServer(1);

          System.out.println("XXX Stop active server[0]");
          manager.stopServer(0);

          // clean up passive dirty db
          System.out.println("XXX Clean passive db dirty bit");
          String[] args = new String[1];
          args[0] = manager.getConfigCreator().getDataLocation(1) + File.separator + "objectdb";
          BaseCommand cmd = new SetDbCleanCommand(new OutputStreamWriter(System.out));
          cmd.execute(args);

          System.out.println("XXX Start passive server[1] as active");
          manager.startServer(1);
          activeIndex = manager.getAndUpdateActiveIndex();
          Assert.assertTrue(activeIndex == 1);
        } catch (Exception e) {
          System.out.println("customerizeActivePassiveTest: " + e);
        }
      }
    });
    apThread.setDaemon(true);
    apThread.start();
  }
}
