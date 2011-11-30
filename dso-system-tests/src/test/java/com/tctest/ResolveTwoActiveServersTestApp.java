/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.JMXUtils;
import com.tc.util.Assert;
import com.tc.util.TCAssertionError;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.IOException;
import java.util.Random;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class ResolveTwoActiveServersTestApp extends AbstractErrorCatchingTransparentApp {
  private String[]              myArrayTestRoot;
  final private String[]        stringAry = { "hee", "hoo", "haa", "terracotta", "google", "yahoo", "apple" };
  final private static long     runtime   = 1000 * 200;
  private final CyclicBarrier   barrier   = new CyclicBarrier(getParticipantCount());

  private final ServerControl[] serverControls;
  private final TCPProxy[]      proxies;
  private final JMXConnector[]  jmxConnectors;

  public ResolveTwoActiveServersTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

    myArrayTestRoot = new String[] { "hee", "hoo", "haa", "terracotta", "google", "yahoo", "apple" };

    proxies = cfg.getProxies();
    serverControls = cfg.getServerControls();
    Assert.assertNotNull(proxies);
    Assert.assertNotNull(serverControls);
    Assert.assertEquals(serverControls.length, 2);

    jmxConnectors = new JMXConnector[2];
  }

  @Override
  public void runTest() throws Exception {
    final boolean isMasterNode = barrier.barrier() == 0;

    println("Starting app work thread...");

    Thread t = new Thread(new Runnable() {
      public void run() {
        doWork();
      }
    });
    t.setName("app-work");
    t.start();

    println("App work thread started.");

    Thread.sleep(5 * 1000);

    if (isMasterNode) {
      createJMXConnectors();
      ensureActiveServer(0);
      ensurePassiveServer(1);
      disconnectServers();
      ensureActiveServer(0);
      ensureActiveServer(1);
      connectServers();
      ensureActiveServer(0);
      ensureDeadServer(1);
    }

    barrier.barrier();

    println("Waiting for app work thread to finish...");
    t.join();

    cleanUp();
  }

  public void cleanUp() throws IOException {
    for (TCPProxy proxie : proxies) {
      proxie.stop();
    }

    for (JMXConnector jmxConnector : jmxConnectors) {
      jmxConnector.close();
    }
  }

  private void connectServers() throws IOException {
    for (TCPProxy proxie : proxies) {
      proxie.start();
    }
  }

  private void disconnectServers() throws IOException {
    for (TCPProxy proxie : proxies) {
      proxie.fastStop();
    }

    MBeanServerConnection mBeanServer = jmxConnectors[1].getMBeanServerConnection();
    TCServerInfoMBean m = MBeanServerInvocationHandler
        .newProxyInstance(mBeanServer, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, true);
    while (true) {
      if (m.isActive()) {
        break;
      }
    }
  }

  private void ensureDeadServer(int index) throws Exception {
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
  }

  private void ensurePassiveServer(int index) throws IOException {
    TCServerInfoMBean m = getJmxServer(index);
    Assert.assertTrue(m.isPassiveStandby());
  }

  private void ensureActiveServer(int index) throws IOException {
    MBeanServerConnection mBeanServer = jmxConnectors[index].getMBeanServerConnection();
    TCServerInfoMBean m = MBeanServerInvocationHandler
        .newProxyInstance(mBeanServer, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, true);
    Assert.assertTrue(m.isActive());
  }

  private TCServerInfoMBean getJmxServer(int index) throws IOException {
    MBeanServerConnection mBeanServer = jmxConnectors[index].getMBeanServerConnection();
    return MBeanServerInvocationHandler.newProxyInstance(mBeanServer, L2MBeanNames.TC_SERVER_INFO,
                                                                             TCServerInfoMBean.class, true);
  }

  private void createJMXConnectors() throws Exception {
    for (int i = 0; i < serverControls.length; i++) {
      jmxConnectors[i] = JMXUtils.getJMXConnector("localhost", serverControls[i].getAdminPort());
    }
  }

  private void println(String s) {
    System.out.println(s);
  }

  // app work related code
  private void doWork() {
    Random rand = new Random();
    long end = System.currentTimeMillis() + runtime;
    try {
      while (end > System.currentTimeMillis()) {
        synchronized (myArrayTestRoot) {
          int idx = rand.nextInt(myArrayTestRoot.length);
          // System.out.println(myArrayTestRoot[rand.nextInt(myArrayTestRoot.length)]);
          Assert.assertTrue(myArrayTestRoot[idx].equals(stringAry[idx]));
        }
        Thread.sleep((int) (Math.random() * 10));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    arrayIndexTestCase();

    testNullArrayAccess();
  }

  private void testNullArrayAccess() {
    Object[] o = returnNull();

    try {
      if (o[3] == null) { throw new AssertionError(); }
    } catch (NullPointerException npe) {
      // expected
    }
  }

  // This method is there to suppress Eclipse warning
  private Object[] returnNull() {
    return null;
  }

  private void arrayIndexTestCase() {
    // We had a bug where ArrayIndexOutOfBoundsException failed to release a monitor, this is the test case for it
    try {
      for (int i = 0; true; i++) {
        Object o = myArrayTestRoot[i];

        // silence warning about unread local variable
        if (o == null) {
          continue;
        }
      }
    } catch (ArrayIndexOutOfBoundsException aioobe) {
      //
    }

    try {
      readArray(myArrayTestRoot, -1);
    } catch (ArrayIndexOutOfBoundsException aioobe) {
      //
    }

  }

  private String readArray(String[] array, int i) {
    return array[i];
  }

  public void setArray(String[] blah) {
    myArrayTestRoot = blah;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ArrayTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("myArrayTestRoot", "myArrayTestRoot");

  }
}
