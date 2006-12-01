/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.gcrunner;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * Application that runs gc by interacting with ObjectManagementMonitorMBean. Expects 2 args: (1) hostname of machine
 * running DSO server (2) jmx server port number
 */
public class GCRunner {
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  public static void main(String[] args) throws IOException {
    if (args == null || args.length != 2) {
      usage();
      return;
    }

    String hostName = args[0];
    int jmxPort = Integer.parseInt(args[1]);

    final JMXConnector jmxConnector = getJMXConnector(hostName, jmxPort);
    final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    ObjectManagementMonitorMBean mbean = (ObjectManagementMonitorMBean) MBeanServerInvocationHandler
        .newProxyInstance(mbs, L2MBeanNames.OBJECT_MANAGEMENT, ObjectManagementMonitorMBean.class, false);

    try {
      mbean.runGC();
    } catch (RuntimeException re) {
      consoleLogger.error(re.getCause().getMessage());
    }
  }

  private static JMXConnector getJMXConnector(String hostName, int jmxPort) throws IOException {
    JMXServiceURL jmxServerUrl = new JMXServiceURL("jmxmp", hostName, jmxPort);
    JMXConnector jmxConnector = JMXConnectorFactory.newJMXConnector(jmxServerUrl, null);
    jmxConnector.connect();
    return jmxConnector;
  }

  private static void usage() {
    consoleLogger.error("Please indicate hostname and jmxport when running this script.");
  }
}
