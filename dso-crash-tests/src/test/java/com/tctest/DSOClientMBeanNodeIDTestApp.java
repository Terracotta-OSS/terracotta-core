/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.cluster.DsoCluster;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.stats.api.DSOClientMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public class DSOClientMBeanNodeIDTestApp extends AbstractTransparentApp {
  private final ApplicationConfig appConfig;

  public DSOClientMBeanNodeIDTestApp(final String appId, final ApplicationConfig cfg,
                                     final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    appConfig = cfg;
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = DSOClientMBeanNodeIDTestApp.class.getName();
    config.addIncludePattern(testClass + "$*", false, false, true);
    config.addWriteAutolock("* " + testClass + "*.*(..)");

    new SynchronizedIntSpec().visit(visitor, config);
  }

  public void run() {
    DSOClientMBeanCoordinator coordinator = new DSOClientMBeanCoordinator();
    coordinator.startDSOClientMBeanCoordinator();
  }

  private class DSOClientMBeanCoordinator {
    @InjectedDsoInstance
    private DsoCluster            cluster;
    private DSOMBean              dsoMBean;
    private MBeanServerConnection mbsc;

    public void startDSOClientMBeanCoordinator() {
      int jmxPort = Integer.valueOf(appConfig.getAttribute(ApplicationConfig.JMXPORT_KEY));
      try {
        JMXConnector jmxc = JMXUtils.getJMXConnector("localhost", jmxPort);
        mbsc = jmxc.getMBeanServerConnection();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
      dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);

      DSOClientMBean[] clients = getDSOClientMBeans();

      Assert.eval(dsoMBean.getClients().length == 1);
      Assert.eval(clients[0].getNodeID().equals(cluster.getCurrentNode().getId()));
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
  }
}
