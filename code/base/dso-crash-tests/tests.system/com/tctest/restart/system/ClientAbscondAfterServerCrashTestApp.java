/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class ClientAbscondAfterServerCrashTestApp extends AbstractTransparentApp implements DsoClusterListener {

  public static String              HOST_NAME     = "host-name";
  public static String              DSO_PORT      = "dso-port";
  public static String              ADMIN_PORT    = "jmx-port";
  public static String              CONFIG_FILE   = "config-file";
  public static String              CLIENT1_SPACE = "client1-workspace";
  public static String              CLIENT2_SPACE = "client2-workspace";

  private final SynchronizedInt     nodesJoined   = new SynchronizedInt(0);
  private final SynchronizedBoolean serverCrashed = new SynchronizedBoolean(false);

  private ServerControl             myBoss;
  private final ApplicationConfig   appConfig;
  private ExtraL1ProcessControl     client1, client2;

  public ClientAbscondAfterServerCrashTestApp(final String appId, final ApplicationConfig cfg,
                                              final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    appConfig = cfg;
    ManagerUtil.getManager().getDsoCluster().addClusterListener(this);
  }

  public void run() {
    // Extra L1s
    List jvmArgs = new ArrayList();
    client1 = new ExtraL1ProcessControl(appConfig.getAttribute(HOST_NAME), Integer.parseInt(appConfig
        .getAttribute(DSO_PORT)), AbscondingClient.class,
                                        new File(appConfig.getAttribute(CONFIG_FILE)).getAbsolutePath(),
                                        Arrays.asList("AbscondingClient"), new File(
                                                                                    appConfig
                                                                                        .getAttribute(CLIENT1_SPACE)),
                                        jvmArgs);

    client2 = new ExtraL1ProcessControl(appConfig.getAttribute(HOST_NAME), Integer.parseInt(appConfig
        .getAttribute(DSO_PORT)), AbscondingClient.class,
                                        new File(appConfig.getAttribute(CONFIG_FILE)).getAbsolutePath(),
                                        Arrays.asList("Resident Client"), new File(
                                                                                   appConfig
                                                                                       .getAttribute(CLIENT2_SPACE)),
                                        jvmArgs);

    try {
      client1.start();
      client2.start();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    // Coordinator L1
    coordinator();
  }

  private void coordinator() {

    // Wait till all clients join the game
    try {
      // checkServerHasClients waits till the clients join
      checkServerHasClients(3, Integer.parseInt(appConfig.getAttribute(ADMIN_PORT)));
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    myBoss = appConfig.getServerControl();
    try {
      System.out.println("XXX Crashing the Server");
      myBoss.crash();
      if (myBoss.isRunning()) throw new AssertionError("Server is still running even after crash.");
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    // Stop one of the extra clients before the server boots-up from the crash
    try {
      System.out.println("XXX Client1 Absconds...");
      client1.attemptShutdown();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    try {
      System.err.println("Re-starting Server...");
      myBoss.start();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    // Let Server complete the reconnect window; This test uses 15 secs reconnect window
    ThreadUtil.reallySleep(15 * 1000);
    // buffer time
    ThreadUtil.reallySleep(5 * 1000);
    try {
      checkServerHasClients(2, Integer.parseInt(appConfig.getAttribute(ADMIN_PORT)));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public static class AbscondingClient {
    public static void main(final String args[]) {
      System.out.println("XXX CLIENT" + args[0] + "STARTED");
      try {
        Thread.sleep(Long.MAX_VALUE);
      } catch (InterruptedException e) {
        //
      }
    }
  }

  private void checkServerHasClients(final int clientCount, final int jmxPort) throws Exception {
    JMXConnector jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
    MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    DSOMBean mbean = (DSOMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DSO, DSOMBean.class,
                                                                              true);
    int actualClientCount = mbean.getClients().length;
    while (actualClientCount != clientCount) {
      System.out.println("XXX Expecting " + clientCount + " clients. Present connected clients " + actualClientCount
                         + ". sleeping ...");
      ThreadUtil.reallySleep(5000);
      actualClientCount = mbean.getClients().length;
    }
    System.out.println("XXX " + clientCount + " clients are connected to the server.");
    jmxConnector.close();
  }

  public void nodeJoined(final DsoClusterEvent event) {
    System.out.println("XXX Node Joined");
    nodesJoined.increment();
  }

  public void nodeLeft(final DsoClusterEvent event) {
    System.out.println("XXX Node Left");
    nodesJoined.decrement();
  }

  public void operationsDisabled(final DsoClusterEvent event) {
    System.out.println("XXX Operations Disabled");
    serverCrashed.set(true);
    nodesJoined.decrement();
  }

  public void operationsEnabled(final DsoClusterEvent event) {
    System.out.println("XXX Operations Enabled");
    nodesJoined.increment();
  }
}
