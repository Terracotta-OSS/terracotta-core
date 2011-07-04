/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.stats.api.DSOClientMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tctest.runner.AbstractTransparentApp;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public class JmxMaxThreadsTestApp extends AbstractTransparentApp {
  public static final String JMX_PORT = "jmx_port";
  private static int         jmxPort;

  public JmxMaxThreadsTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    jmxPort = Integer.parseInt(config.getAttribute(JMX_PORT));
  }

  public void run() {
    // Create threads and get l1 infos
    int noOfThreads = 20;

    Thread[] threads = new Thread[noOfThreads];
    for (int i = 0; i < noOfThreads; i++) {
      threads[i] = new Thread(new Runnable() {
        public void run() {
          getConfig();
        }
      });
    }

    for (int i = 0; i < noOfThreads; i++) {
      threads[i].start();
    }

    for (int i = 0; i < noOfThreads; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        //
      }
    }
  }

  private static void getConfig() {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
    } catch (Exception e) {
      new AssertionError(e);
    }
    MBeanServerConnection mbsc = getMBeanServerConnection(jmxConnector, "localhost", jmxPort);
    DSOMBean dsoMBean = (DSOMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO,
                                                                                 DSOMBean.class, false);

    ObjectName[] clientObjectNames = dsoMBean.getClients();
    DSOClientMBean[] clients = new DSOClientMBean[clientObjectNames.length];
    for (int i = 0; i < clients.length; i++) {
      clients[i] = (DSOClientMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, clientObjectNames[i],
                                                                                  DSOClientMBean.class, false);
      L1InfoMBean l1InfoMbean = (L1InfoMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, clients[i]
          .getL1InfoBeanName(), L1InfoMBean.class, false);
      l1InfoMbean.getConfig();
    }
    closeJMXConnector(jmxConnector);
  }

  private static MBeanServerConnection getMBeanServerConnection(final JMXConnector jmxConnector, String host, int port) {
    MBeanServerConnection mbs;
    try {
      mbs = jmxConnector.getMBeanServerConnection();
    } catch (IOException e1) {
      System.err.println("Unable to connect to host '" + host + "', port " + port
                         + ". Are you sure there is a Terracotta Server instance running there?");
      return null;
    }
    return mbs;
  }

  private static void closeJMXConnector(final JMXConnector jmxConnector) {
    try {
      jmxConnector.close();
    } catch (IOException e) {
      System.err.println("Unable to close the JMX connector " + e.getMessage());
    }
  }
}
