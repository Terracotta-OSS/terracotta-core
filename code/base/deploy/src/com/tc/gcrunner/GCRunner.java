/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.gcrunner;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

/**
 * Application that runs gc by interacting with ObjectManagementMonitorMBean. Expects 2 args: (1) hostname of machine
 * running DSO server (2) jmx server port number
 */
public class GCRunner {
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  private String                m_host;
  private int                   m_port;
  private String                m_userName;

  public static final String    DEFAULT_HOST  = "localhost";
  public static final int       DEFAULT_PORT  = 9520;

  public static void main(String[] args) throws Exception {

    CommandLineBuilder comandLineBuilder = new CommandLineBuilder(GCRunner.class.getName(), args);

    comandLineBuilder.addOption("n", "hostname", true, "Terracotta Server hostname", String.class, false, "l2-hostname");
    comandLineBuilder.addOption("p", "jmxport", true, "Terracotta Server JMX port", Integer.class, false, "l2-jmx-port");
    comandLineBuilder.addOption("u", "username", true, "user name", String.class, false);
    comandLineBuilder.addOption("h", "help", String.class, false);

    comandLineBuilder.parse();
    comandLineBuilder.printArguments();

    String[] arguments = comandLineBuilder.getArguments();
    if (arguments.length > 2) {
      comandLineBuilder.usageAndDie();
    }

    if (comandLineBuilder.hasOption('h')) {
      comandLineBuilder.usageAndDie();
    }

    String userName = null;
    if (comandLineBuilder.hasOption('u')) {
      userName = comandLineBuilder.getOptionValue('u');
    }

    String host = null;
    int port = -1;

    if (arguments.length == 0) {
      host = DEFAULT_HOST;
      port = DEFAULT_PORT;
      System.err.println("No host or port provided. Invoking GC on Terracotta server at '" + host + "', port " + port
                         + " by default.");
    } else if (arguments.length == 1) {
      host = DEFAULT_HOST;
      try {
        port = Integer.parseInt(arguments[0]);
      } catch (NumberFormatException e) {
        port = DEFAULT_PORT;
        System.err.println("Invalid port number specified. Using default port '" + port + "'");
      }
    } else {
      host = arguments[0];
      port = Integer.parseInt(arguments[1]);
    }

    try {
      new GCRunner(host, port, userName).runGC();
    } catch (IOException ioe) {
      System.err.println("Unable to connect to host '" + host + "', port " + port
                         + ". Are you sure there is a Terracotta server running there?");
    } catch (SecurityException se) {
      System.err.println(se.getMessage());
      comandLineBuilder.usageAndDie();
    }
  }

  public GCRunner(String host, int port) {
    m_host = host;
    m_port = port;
  }

  public GCRunner(String host, int port, String userName) {
    this(host, port);
    m_userName = userName;
  }

  private void runGC() throws Exception {
    ObjectManagementMonitorMBean mbean = null;
    final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector(m_userName, m_host, m_port);
    final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    mbean = MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.OBJECT_MANAGEMENT,
                                                     ObjectManagementMonitorMBean.class, false);
    try {
      mbean.runGC();
    } catch (RuntimeException e) {
      // DEV-1168
      consoleLogger.error((e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
    }
  }
}
