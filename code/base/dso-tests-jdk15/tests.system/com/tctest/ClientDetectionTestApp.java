/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.stats.DSOClientMBean;
import com.tc.stats.DSOMBean;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

public class ClientDetectionTestApp extends AbstractErrorCatchingTransparentApp {
  public static final String    CONFIG_FILE = "config-file";
  public static final String    PORT_NUMBER = "port-number";
  public static final String    HOST_NAME   = "host-name";
  public static final String    JMX_PORT    = "jmx-port";

  private ApplicationConfig     appConfig;
  private JMXConnectorProxy     jmxc;
  private MBeanServerConnection mbsc;
  private DSOMBean              dsoMBean;
  
  private CyclicBarrier         barrier4;
  private CyclicBarrier         barrier3;
  private CyclicBarrier         barrier2;

  public ClientDetectionTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    appConfig = cfg;

    barrier4 = new CyclicBarrier(4);
    barrier3 = new CyclicBarrier(3);
    barrier2 = new CyclicBarrier(2);
  }

  protected void runTest() throws Throwable {
    jmxc = new JMXConnectorProxy("localhost", Integer.valueOf(appConfig.getAttribute(JMX_PORT)));
    mbsc = jmxc.getMBeanServerConnection();

    Manageable b4 = (Manageable)barrier4;
    System.out.println("@@@@@@ Testapp b4: " + b4.__tc_managed().getObjectID());
    
    ExtraL1ProcessControl client1 = spawnNewClient(1);
    ExtraL1ProcessControl client2 = spawnNewClient(2);
    ExtraL1ProcessControl client3 = spawnNewClient(3);
    
    System.out.println("@@@@@@@@@@ spawned 3 more clients");    
    
    System.out.println("Main test calling barrier4...");
    barrier4.await();
    assertClientPresent(getDSOClientMBeans(), 4);
    barrier4.await();
    client3.attemptShutdown();

    barrier3.await();    
    assertClientPresent(getDSOClientMBeans(), 3);
    barrier3.await();
    client2.attemptShutdown();

    barrier2.await();
    assertClientPresent(getDSOClientMBeans(), 2);
    barrier2.await();
    client1.attemptShutdown();

    assertClientPresent(getDSOClientMBeans(), 1);
  }

  private DSOClientMBean[] getDSOClientMBeans() {    
    dsoMBean = (DSOMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
    ObjectName[] clientObjectNames = dsoMBean.getClients();
    DSOClientMBean[] clients = new DSOClientMBean[clientObjectNames.length];
    for (int i = 0; i < clients.length; i++) {
      clients[i] = (DSOClientMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, clientObjectNames[i],
                                                                                  DSOClientMBean.class, false);
    }
    return clients;
  }

  private void assertClientPresent(DSOClientMBean[] clientMBeans, int expectedCount) throws Exception {
    ThreadUtil.reallySleep(5000);
    System.out.println("assertClientPresent: expectedCount = " + expectedCount);
    Set set = new HashSet<String>();
    for (DSOClientMBean bean : clientMBeans) {
      set.add(bean.getChannelID().toString() + bean.getRemoteAddress());
    }
    System.out.println(set);
    Assert.assertEquals(expectedCount, clientMBeans.length);
    Assert.assertEquals(expectedCount, set.size());
  }

  private ExtraL1ProcessControl spawnNewClient(int id) throws Exception {
    final String hostName = appConfig.getAttribute(HOST_NAME);
    final int port = Integer.parseInt(appConfig.getAttribute(PORT_NUMBER));
    final File configFile = new File(appConfig.getAttribute(CONFIG_FILE));
    File workingDir = new File(configFile.getParentFile(), "client-" + id);
    FileUtils.forceMkdir(workingDir);

    List jvmArgs = new ArrayList();
    addTestTcPropertiesFile(jvmArgs);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, L1Client.class, configFile
        .getAbsolutePath(), new String[0], workingDir, jvmArgs);
    client.start();
    
    System.err.println("Client " + id + " finished");
    
    client.mergeSTDERR();
    client.mergeSTDOUT();    
    
    return client;
  }

  private void addTestTcPropertiesFile(List jvmArgs) {
    URL url = getClass().getResource("/com/tc/properties/tests.properties");
    if (url == null) { return; }
    String pathToTestTcProperties = url.getPath();
    if (pathToTestTcProperties == null || pathToTestTcProperties.equals("")) { return; }
    jvmArgs.add("-Dcom.tc.properties=" + pathToTestTcProperties);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

    String testClass = ClientDetectionTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*", false, false, true);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier4", "barrier4");
    spec.addRoot("barrier3", "barrier3");
    spec.addRoot("barrier2", "barrier2");

  }

  public static class L1Client {
    CyclicBarrier barrier4 = new CyclicBarrier(4);
    CyclicBarrier barrier3 = new CyclicBarrier(3);
    CyclicBarrier barrier2 = new CyclicBarrier(2);

    public static void main(String args[]) throws Exception {
      System.out.println("@@@@@@@ I'm online....");
      L1Client l1 = new L1Client();

      l1.barrier4.await();
      l1.barrier4.await();
      
      l1.barrier3.await();
      l1.barrier3.await();
      
      l1.barrier2.await();
      l1.barrier2.await();

    }
  }

}
