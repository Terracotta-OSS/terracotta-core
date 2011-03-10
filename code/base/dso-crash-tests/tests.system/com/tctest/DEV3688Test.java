/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.test.GroupConfigBuilder;
import com.tc.config.schema.test.GroupsConfigBuilder;
import com.tc.config.schema.test.MembersConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.TestConfigObject;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.Assert;
import com.tctest.DEV3688App.DEV3688Worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Steps to reproduce DEV-3688:<br>
 * <br>
 * 1. Start A which becomes ACTIVE<br>
 * 2. Start a client which creates root and does something<br>
 * 3. Stop A<br>
 * 4. Start A again which will again start as ACTIVE<br>
 * 5. Start B which will become PASSIVE<br>
 * 6. Stop A so B becomes ACTIVE<br>
 * 7. Start a new client now and you will see that the server has again given ObjectID=1000<br>
 */
public class DEV3688Test extends ActivePassiveTransparentTestBase {

  /**
   * Don't change this value
   */
  private static final int NODE_COUNT = 1;

  public DEV3688Test() {
    // disableAllUntil(new Date(Long.MAX_VALUE));
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return DEV3688App.class;
  }

  @Override
  protected boolean enableL1Reconnect() {
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
  protected boolean canRun() {
    return (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE));
  }

  @Override
  protected void customizeActivePassiveTest(final ActivePassiveServerManager manager) throws Exception {
    /**
     * Steps to reproduce DEV-3688:<br>
     * <br>
     * 1. Start A which becomes ACTIVE<br>
     * 2. Start a client which creates root and does something<br>
     * 3. Stop A<br>
     * 4. Start A again which will again start as ACTIVE<br>
     * 5. Start B which will become PASSIVE<br>
     * 6. Stop A so B becomes ACTIVE<br>
     * 7. Start a new client now and you will see that the server has again given ObjectID=1000<br>
     */

    System.out.println("1. Start A which becomes ACTIVE");
    manager.startServer(0);

    // Allow L1/clients to start, do rest in a thread
    Thread apThread = new Thread(new Runnable() {
      int activeIndex;

      public void run() {
        try {
          Thread.sleep(5000);
          activeIndex = manager.getAndUpdateActiveIndex();
          Assert.assertTrue(activeIndex == 0);
          System.out.println("3. Stop A");
          manager.stopServer(0);

          Thread.sleep(100);
          System.out.println("4. Start A again which will again start as ACTIVE");
          manager.startServer(0);
          activeIndex = manager.getAndUpdateActiveIndex();
          Assert.assertTrue(activeIndex == 0);

          System.out.println("5. Start B which will become PASSIVE");
          manager.startServer(1);
          Thread.sleep(1000);
          manager.waitServerIsPassiveStandby(1, 20);

          System.out.println("6. Stop A so B becomes ACTIVE");
          manager.stopServer(0);
          Thread.sleep(2000);
          activeIndex = manager.getAndUpdateActiveIndex();
          Assert.assertTrue(activeIndex == 1);

          System.out.println("7. Start a new client now and the server should not give ObjectID=1000");

          int currentActiveDsoPort = manager.getDsoPorts()[1];
          List jvmArgs = new ArrayList();
          jvmArgs.add("-Dtc.node-name=my-client-2");
          jvmArgs.add("-Dcom.tc.l1.l2.config.validation.enabled=false");
          String configFile = createConfigFile(manager, 1);
          File workDir = new File(getTempDirectory(), "my-client-2");
          workDir.mkdirs();
          ExtraL1ProcessControl client = new ExtraL1ProcessControl("localhost", currentActiveDsoPort,
                                                                   DEV3688Worker.class, configFile,
                                                                   Collections.EMPTY_LIST, workDir, jvmArgs);
          client.start();
          client.mergeSTDERR();
          client.mergeSTDOUT();
          client.waitUntilShutdown();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    apThread.setDaemon(true);
    apThread.start();
  }

  private String createConfigFile(ActivePassiveServerManager manager, int index) {
    TerracottaConfigBuilder cb = DEV3688App.getTerracottaConfigBuilder();
    cb.getServers().getL2s()[0].setDSOPort(manager.getDsoPorts()[index]);
    cb.getServers().getL2s()[0].setJMXPort(manager.getJmxPorts()[index]);
    cb.getServers().getL2s()[0].setName(manager.getServerNames()[index]);

    MembersConfigBuilder members = new MembersConfigBuilder();
    members.addMember(manager.getServerNames()[index]);

    GroupConfigBuilder group = new GroupConfigBuilder(manager.getGroupName());
    group.setMembers(members);

    GroupsConfigBuilder groups = new GroupsConfigBuilder();
    groups.addGroupConfigBuilder(group);

    cb.getServers().setGroups(groups);
    try {
      File tmpConfig = getTempFile("config-file.xml");
      FileOutputStream fileOutputStream = new FileOutputStream(tmpConfig);
      PrintWriter out = new PrintWriter((fileOutputStream));
      out.println(cb.toString());
      out.flush();
      out.close();
      return tmpConfig.getAbsolutePath();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
