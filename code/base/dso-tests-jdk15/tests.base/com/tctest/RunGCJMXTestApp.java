/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class RunGCJMXTestApp extends AbstractTransparentApp {

  public static final String           CONFIG_FILE = "config-file";
  public static final String           PORT_NUMBER = "port-number";
  public static final String           HOST_NAME   = "host-name";
  public static final String           JMX_PORT    = "jmx-port";

  private final ApplicationConfig      config;

  private MBeanServerConnection        mbsc        = null;
  private JMXConnector                 jmxc;
  private ObjectManagementMonitorMBean objectMBean;

  public RunGCJMXTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
  }

  public void run() {

    for (int i = 0; i < 10; i++) {
      try {
        runGC();
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
      if (!config.getServerControl().isRunning()) {
        notifyError("call to objectManagementMonitor.runGC() should not stop server");
      }
    }

  }

  private void connect() throws Exception {
    System.out.println("connecting to jmx server....");
    jmxc = new JMXConnectorProxy("localhost", Integer.parseInt(config.getAttribute(JMX_PORT)));
    mbsc = jmxc.getMBeanServerConnection();
    System.out.println("obtained mbeanserver connection");
    objectMBean = (ObjectManagementMonitorMBean) MBeanServerInvocationHandler
        .newProxyInstance(mbsc, L2MBeanNames.OBJECT_MANAGEMENT, ObjectManagementMonitorMBean.class, false);

  }

  private void disconnect() throws Exception {
    if (jmxc != null) {
      jmxc.close();
    }
  }

  private void runGC() throws Exception {
      connect();
      objectMBean.runGC();
      disconnect();   
  }

}
