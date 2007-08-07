/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  public static void main(String[] args) {
    if (args == null || args.length != 2) {
      usage();
      return;
    }

    String hostName = args[0];
    int jmxPort = Integer.parseInt(args[1]);

    ObjectManagementMonitorMBean mbean = null;
    try {
      final JMXConnector jmxConnector = getJMXConnector(hostName, jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = (ObjectManagementMonitorMBean) MBeanServerInvocationHandler
          .newProxyInstance(mbs, L2MBeanNames.OBJECT_MANAGEMENT, ObjectManagementMonitorMBean.class, false);
    } catch (Exception e) {
      consoleLogger.error("Error occurred connecting to DSO server.");
      System.exit(1);
    }

    try {
      mbean.runGC();
    } catch (RuntimeException re) {
      consoleLogger.error(re.getCause().getMessage());
    }
  }

  private static JMXConnector getJMXConnector(String hostName, int jmxPort) throws IOException {
    String url = "service:jmx:rmi:///jndi/rmi://" + hostName + ":" + jmxPort + "/jmxrmi";
    JMXServiceURL jmxServerUrl = new JMXServiceURL(url);
    JMXConnector jmxConnector = JMXConnectorFactory.newJMXConnector(jmxServerUrl, null);
    jmxConnector.connect();
    return jmxConnector;
  }

  private static void usage() {

    consoleLogger
        .error("This script runs DGC in the indicated DSO server when GC is not enabled through config file and DGC is not already running on the DSO server.\n"
               + "        Usage:  run-dgc.sh/.bat [hostname] [jmxport]\n\n"
               + "        hostname     IP address or hostname of DSO server machine (e.g., 127.0.0.1)\n"
               + "        jmxport      JMX port number associated with DSO server (e.g., 9520)");
  }
}
