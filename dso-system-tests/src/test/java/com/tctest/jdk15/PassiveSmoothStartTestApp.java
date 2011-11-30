/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.lang.ServerExitStatus;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.TCAssertionError;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class PassiveSmoothStartTestApp extends AbstractTransparentApp {
  private final ServerControl[]   serverControls;
  private final ApplicationConfig appConfig;
  private final JMXConnector[]    jmxConnectors;
  public static String            SERVER0_DATA_PATH = "server0_data_path";
  public static String            SERVER1_DATA_PATH = "server1_data_path";

  public PassiveSmoothStartTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.appConfig = cfg;
    this.serverControls = cfg.getServerControls();
    Assert.assertNotNull(this.serverControls);
    Assert.eval(this.serverControls.length == 2);
    jmxConnectors = new JMXConnector[2];
  }

  public void run() {

    // Initially,
    // 0 - Active Server Index
    // 1 - Passive Server Index

    checkClusterStates(0, 1);

    File dataHome0 = new File(appConfig.getAttribute(SERVER0_DATA_PATH));
    File objectDB0 = new File(dataHome0, L2DSOConfig.OBJECTDB_DIRNAME);
    File dirtyObjectDB0 = new File(dataHome0, L2DSOConfig.DIRTY_OBJECTDB_BACKUP_DIRNAME);

    File dataHome1 = new File(appConfig.getAttribute(SERVER1_DATA_PATH));
    File objectDB1 = new File(dataHome1, L2DSOConfig.OBJECTDB_DIRNAME);
    File dirtyObjectDB1 = new File(dataHome1, L2DSOConfig.DIRTY_OBJECTDB_BACKUP_DIRNAME);

    // Pre-test verifications
    Assert.eval(dataHome1.exists());
    Assert.eval(objectDB1.exists());
    Assert.eval(!dirtyObjectDB1.exists());

    Assert.eval(dataHome0.exists());
    Assert.eval(objectDB0.exists());
    Assert.eval(!dirtyObjectDB0.exists());

    // Test-1 : Crash the passive and restart once. while restarting, dirty-object-db wouldn't allow it to come up. but
    // with RMP-309, it should solve the problem on its own and come back to passive stand-by position
    CrashServerNodeAndRestart(1);

    // Verification 1: parent dirty-objectdb-backup directory created ??
    while (!dirtyObjectDB1.exists()) {
      ThreadUtil.reallySleep(5000);
      System.out.println("XXX waiting for crashed server to create backup for dirty db");
    }

    // Verification 2: dirty-objectdb-<timestamp> directory created ??
    verifyDirtyObjectDbBackupDirs(dirtyObjectDB1, 1);

    checkClusterStates(0, 1);

    // Test-2 : Repeating test 1 again, to check if the timestamped back-ups are happening
    CrashServerNodeAndRestart(1);

    // Verification 3: dirty-objectdb-<timestamp> directory created ??
    verifyDirtyObjectDbBackupDirs(dirtyObjectDB1, 2);

    checkClusterStates(0, 1);

    // Test-3 : Crash the Active and restart once. Actually speaking, crashing the server node and restarting it when
    // there is no shared objects by the client should not trigger any dirty object db problems. The old active should
    // be able to join the cluster back as a passive even without RMP-309. but since the test framework uses some shared
    // objects, dirty-db problem is expected to happen.
    CrashServerNodeAndRestart(0);

    // Now,
    // 1 - Active Server Index
    // 0 - Passive Server Index

    // Verification 1: parent dirty-objectdb-backup directory created ??
    while (!dirtyObjectDB0.exists()) {
      ThreadUtil.reallySleep(5000);
      System.out.println("XXX waiting for crashed server to create backup for dirty db");
    }

    // Verification 2: dirty-objectdb-<timestamp> directory created ??
    verifyDirtyObjectDbBackupDirs(dirtyObjectDB0, 1);

    checkClusterStates(1, 0);

    // Test-4 : Crash the new Passive
    CrashServerNodeAndRestart(0);

    // Verification 3: dirty-objectdb-<timestamp> directory created ??
    verifyDirtyObjectDbBackupDirs(dirtyObjectDB0, 2);

    checkClusterStates(1, 0);

    System.out.println("XXX Success");
    return;
  }

  private void CrashServerNodeAndRestart(int serverIndexp) {
    try {
      int exitCode;
      serverControls[serverIndexp].crash();
      System.out.println("XXX Crashed passive purposefully");

      ensureDeadServer(serverIndexp);
      serverControls[serverIndexp].start();
      System.out.println("XXX Passive .. 'I am back'");
      exitCode = serverControls[serverIndexp].waitFor();

      System.out.println("XXX Passvie crashed while booting-up as expected. ExitCode = " + exitCode);

      // Simulating start-up script behavior
      Assert.eval(exitCode == ServerExitStatus.EXITCODE_RESTART_REQUEST);
      serverControls[serverIndexp].start();
    } catch (Exception e1) {
      throw new RuntimeException(e1);
    }

    ThreadUtil.reallySleep(10000);
  }

  private void verifyDirtyObjectDbBackupDirs(File dirtyObjectDB, int expectedBackupCount) {
    File[] dirtyObjectDBTimeStampedDirs = dirtyObjectDB.listFiles();

    while (dirtyObjectDBTimeStampedDirs.length != expectedBackupCount) {
      System.out.println("XXX waiting for data backup dir creation. current backups: "
                         + dirtyObjectDBTimeStampedDirs.length + "; expected: " + expectedBackupCount);
      ThreadUtil.reallySleep(5000);
    }

    for (File dirtyObjectDBTimeStampedDir : dirtyObjectDBTimeStampedDirs) {
      Assert.eval(new String(dirtyObjectDBTimeStampedDirs[0].getName())
          .startsWith(L2DSOConfig.DIRTY_OBJECTDB_BACKUP_PREFIX));
      System.out.println("XXX Successfully created Timestamped DirtyObjectDB Backup dir "
                         + dirtyObjectDBTimeStampedDir.getAbsolutePath());
    }
  }

  private void checkClusterStates(int activeIndex, int passiveIndex) {
    createJMXConnectors();
    try {
      ensureActiveServer(activeIndex);
      ensurePassiveServer(passiveIndex);
    } catch (Exception e3) {
      throw new RuntimeException(e3);
    }
    closeJMXConnectors();
  }

  private void ensureDeadServer(int index) throws Exception {
    createJMXConnectors();
    if (serverControls[index].isRunning()) { throw new Exception(
                                                                 "Server control is still running when it should not be!"); }
    try {
      ensureActiveServer(index);
      throw new Exception("The server is active when it should not even be running!");
    } catch (IOException e) {
      // expected
    } catch (TCAssertionError tce) {
      throw new Exception("The server is passive/started when it should not even be running!");
    }
    closeJMXConnectors();
  }

  private void ensurePassiveServer(int index) throws IOException {
    TCServerInfoMBean m = getJmxServer(index);
    while (!m.isPassiveStandby()) {
      System.out.println("XXX waiting for passive to join the cluster");
      ThreadUtil.reallySleep(1000);
    }
  }

  private void ensureActiveServer(int index) throws IOException {
    jmxConnectors[index].connect();
    MBeanServerConnection mBeanServer = jmxConnectors[index].getMBeanServerConnection();
    TCServerInfoMBean m = MBeanServerInvocationHandler
        .newProxyInstance(mBeanServer, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, true);
    while (!m.isActive()) {
      System.out.println("XXX waiting for active to join the cluster");
      ThreadUtil.reallySleep(1000);
    }
  }

  private TCServerInfoMBean getJmxServer(int index) throws IOException {
    jmxConnectors[index].connect();
    MBeanServerConnection mBeanServer = jmxConnectors[index].getMBeanServerConnection();
    return MBeanServerInvocationHandler.newProxyInstance(mBeanServer, L2MBeanNames.TC_SERVER_INFO,
                                                                             TCServerInfoMBean.class, true);
  }

  private void createJMXConnectors() {
    for (int i = 0; i < serverControls.length; i++) {
      try {
        jmxConnectors[i] = newJMXConnector("localhost", serverControls[i].getAdminPort());
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  private void closeJMXConnectors() {
    for (int i = 0; i < serverControls.length; i++) {
      try {
        jmxConnectors[i].close();
      } catch (IOException e) {
        // who cares
      }
    }
  }

  /*
   * This is line AbstractTransparentApp.getJMXConnector except that it doesn't automatically connect.
   */
  private static JMXConnector newJMXConnector(String host, int jmxPort) throws Exception {
    JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://" + host + ":" + jmxPort);
    JMXConnector jmxc = JMXConnectorFactory.newJMXConnector(url, null);
    return jmxc;
  }
}
