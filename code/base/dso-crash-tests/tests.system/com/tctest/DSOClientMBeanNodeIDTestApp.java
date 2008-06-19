/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.cluster.ClusterEventListener;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.stats.DSOClientMBean;
import com.tc.stats.DSOMBean;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

public class DSOClientMBeanNodeIDTestApp extends AbstractTransparentApp {

  public static final String    CONFIG_FILE = "config-file";
  public static final String    PORT_NUMBER = "port-number";
  public static final String    HOST_NAME   = "host-name";
  public static final String    JMX_PORT    = "jmx-port";
  // private static final int TOTAL_L1_PROCESS = 2;

  private ApplicationConfig     appConfig;

  // private final CyclicBarrier barrier = new CyclicBarrier(TOTAL_L1_PROCESS);

  public DSOClientMBeanNodeIDTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    appConfig = cfg;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = DSOClientMBeanNodeIDTestApp.class.getName();
    config.addIncludePattern(testClass + "$*", false, false, true);
    config.addWriteAutolock("* " + testClass + "*.*(..)");

    new SynchronizedIntSpec().visit(visitor, config);
  }

  public void run() {
    DSOClientMBeanCoordinator coordinator = new DSOClientMBeanCoordinator();
    coordinator.startDSOClientMBeanCoordinator();
  }

  private class DSOClientMBeanCoordinator implements ClusterEventListener {
    private DSOMBean              dsoMBean;
    private MBeanServerConnection mbsc;
    private String                nodeID;

    public void startDSOClientMBeanCoordinator() {
      ManagerUtil.addClusterEventListener(this);
      JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", Integer.valueOf(appConfig.getAttribute(JMX_PORT)));
      try {
        mbsc = jmxc.getMBeanServerConnection();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
      dsoMBean = (DSOMBean) MBeanServerInvocationHandler
          .newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);

      DSOClientMBean[] clients = getDSOClientMBeans();

      Assert.eval(dsoMBean.getClients().length == 1);
      Assert.eval(clients[0].getNodeID().equals(this.nodeID));

    }

    private DSOClientMBean[] getDSOClientMBeans() {

      ObjectName[] clientObjectNames = dsoMBean.getClients();
      DSOClientMBean[] clients = new DSOClientMBean[clientObjectNames.length];
      for (int i = 0; i < clients.length; i++) {
        clients[i] = (DSOClientMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, clientObjectNames[i],
                                                                                    DSOClientMBean.class, false);
      }
      return clients;
    }

    public void nodeConnected(String aNodeId) {
      // we are not asserting for connection
      // do nothing
    }

    public void nodeDisconnected(String aNodeId) {
      // node got disconnected
    }

    public void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster) {
      // do nothing
      this.nodeID = thisNodeId;
    }

    public void thisNodeDisconnected(String thisNodeId) {
      // do nothing
    }

  }

}
