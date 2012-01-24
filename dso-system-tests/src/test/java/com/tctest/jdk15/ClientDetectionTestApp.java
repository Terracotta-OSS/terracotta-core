/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import org.apache.commons.io.FileUtils;

import com.tc.management.beans.L2MBeanNames;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.stats.api.DSOClientMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public class ClientDetectionTestApp extends AbstractErrorCatchingTransparentApp implements NotificationListener {
  public static final String      CONFIG_FILE  = "config-file";
  public static final String      PORT_NUMBER  = "port-number";
  public static final String      HOST_NAME    = "host-name";
  public static final String      JMX_PORT     = "jmx-port";

  private final ApplicationConfig appConfig;
  private JMXConnector            jmxc;
  private MBeanServerConnection   mbsc;
  private DSOMBean                dsoMBean;

  private final CyclicBarrier     barrier4;
  private final Set               channelIdSet = new HashSet();

  public ClientDetectionTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    appConfig = cfg;

    barrier4 = new CyclicBarrier(4);
  }

  @Override
  protected void runTest() throws Throwable {
    int jmxPort = Integer.valueOf(appConfig.getAttribute(JMX_PORT));
    jmxc = JMXUtils.getJMXConnector("localhost", jmxPort);
    mbsc = jmxc.getMBeanServerConnection();
    dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
    getCurrentChannelIds();
    mbsc.addNotificationListener(L2MBeanNames.DSO, this, null, null);

    System.out.println("@@@@@@@ I'm online.... id = " + ManagerUtil.getClientID());

    ExtraL1ProcessControl client1 = spawnNewClient(1);
    ExtraL1ProcessControl client2 = spawnNewClient(2);
    ExtraL1ProcessControl client3 = spawnNewClient(3);

    barrier4.await();
    while (getClientsConnected(getDSOClientMBeans()) != 4) {
      ThreadUtil.reallySleep(10000);
    }
    barrier4.await();
    System.out.println("Waiting For Client 1 Termination");
    while (client1.isRunning()) {
      ThreadUtil.reallySleep(1000);
    }
    System.out.println("Waiting For Client 2 Termination");
    while (client2.isRunning()) {
      ThreadUtil.reallySleep(1000);
    }
    System.out.println("Waiting For Client 3 Termination");
    while (client3.isRunning()) {
      ThreadUtil.reallySleep(1000);
    }

    Thread.sleep(30000);
    System.out.println("3 Clients killed");
    while (getClientsConnected(getDSOClientMBeans()) != 1) {
      ThreadUtil.reallySleep(10000);
    }
  }

  private DSOClientMBean[] getDSOClientMBeans() {
    ObjectName[] clientObjectNames = dsoMBean.getClients();
    DSOClientMBean[] clients = new DSOClientMBean[clientObjectNames.length];
    for (int i = 0; i < clients.length; i++) {
      clients[i] = MBeanServerInvocationHandler.newProxyInstance(mbsc, clientObjectNames[i], DSOClientMBean.class,
                                                                 false);
    }
    return clients;
  }

  private void getCurrentChannelIds() {
    for (DSOClientMBean c : getDSOClientMBeans()) {
      channelIdSet.add(String.valueOf(c.getChannelID().toLong()));
    }
  }

  private int getClientsConnected(DSOClientMBean[] clientMBeans) throws Exception {
    Set set = new HashSet<String>();
    for (DSOClientMBean bean : clientMBeans) {
      System.out.println("got channel Id from dso bean: " + bean.getChannelID().toLong());
      set.add(bean.getChannelID().toString() + bean.getRemoteAddress());
    }
    System.out.println("ClientPresent: " + set);
    return set.size();
  }

  private ExtraL1ProcessControl spawnNewClient(int id) throws Exception {
    final String hostName = appConfig.getAttribute(HOST_NAME);
    final int port = Integer.parseInt(appConfig.getAttribute(PORT_NUMBER));
    final File configFile = new File(appConfig.getAttribute(CONFIG_FILE));
    File workingDir = new File(configFile.getParentFile(), "client-" + id);
    FileUtils.forceMkdir(workingDir);

    List jvmArgs = new ArrayList();
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, L1Client.class,
                                                             configFile.getAbsolutePath(), Collections.EMPTY_LIST,
                                                             workingDir, jvmArgs);
    client.start();

    client.mergeSTDERR();
    client.mergeSTDOUT();

    return client;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

    String testClass = ClientDetectionTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*", false, false, true);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier4", "barrier4");
  }

  public static class L1Client {
    CyclicBarrier barrier4 = new CyclicBarrier(4);

    public static void main(String args[]) throws Exception {
      System.out.println("@@@@@@@ I'm online.... id = " + ManagerUtil.getClientID());
      L1Client l1 = new L1Client();

      System.out.println("entering barrier4");
      l1.barrier4.await();
      l1.barrier4.await();
    }
  }

  public void handleNotification(Notification notification, Object handback) {
    if ("dso.client.attached".equalsIgnoreCase(notification.getType())) {
      // extract channel id
      String source = notification.getSource().toString();
      int index = source.lastIndexOf('=');
      String channelId = source.substring(index + 1);

      System.out.println(">>>>> notification of channelId: " + channelId);
      Assert.assertFalse("duplicate clients notification found", channelIdSet.contains(channelId));
      channelIdSet.add(channelId);
    }

  }

}
