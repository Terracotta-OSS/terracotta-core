/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.gcrunner;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;

import java.io.IOException;
import java.net.InetAddress;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

/**
 * Application that runs dgc by interacting with ObjectManagementMonitorMBean. Expects 2 args: (1) hostname of machine
 * running DSO server (2) jmx server port number
 */
public class GCRunner {
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  private String                host;
  private int                   port;
  private String                userName;

  public static final String    DEFAULT_HOST  = "localhost";
  public static final int       DEFAULT_PORT  = 9520;

  public static void main(String[] args) throws Exception {

    CommandLineBuilder comandLineBuilder = new CommandLineBuilder(GCRunner.class.getName(), args);

    comandLineBuilder.addOption("n", "hostname", true, "The Terracotta Server instane hostname", String.class, false,
                                "l2-hostname");
    comandLineBuilder.addOption("p", "jmxport", true, "Terracotta Server instance JMX port", Integer.class, false,
                                "l2-jmx-port");
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
      System.err.println("No host or port provided. Invoking DGC on Terracotta server instance at '" + host
                         + "', port " + port + " by default.");
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
                         + ". Are you sure there is a Terracotta server instance running there?");
    } catch (SecurityException se) {
      System.err.println(se.getMessage());
      comandLineBuilder.usageAndDie();
    }
  }

  public GCRunner(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public GCRunner(String host, int port, String userName) {
    this(host, port);
    this.userName = userName;
  }

  public void runGC() throws Exception {
    if (!setActiveCoordinatorJmxPortAndHost(host, port)) {
      consoleLogger.info("DGC can only be called on server " + host + " with JMX port " + port
                         + ". So the request is being redirected.");
    }

    ObjectManagementMonitorMBean mbean = null;
    final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector(userName, host, port);
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

  private boolean setActiveCoordinatorJmxPortAndHost(String host, int jmxPort) throws Exception {
    ServerGroupInfo[] serverGrpInfos = getServerGroupInfo();
    L2Info[] activeGrpServerInfos = null;
    for (int i = 0; i < serverGrpInfos.length; i++) {
      if (serverGrpInfos[i].isCoordinator()) {
        activeGrpServerInfos = serverGrpInfos[i].members();
      }
    }

    boolean isActiveFound = false;
    for (int i = 0; i < activeGrpServerInfos.length; i++) {
      if (isActive(activeGrpServerInfos[i].host(), activeGrpServerInfos[i].jmxPort())) {
        isActiveFound = true;
        this.host = activeGrpServerInfos[i].host();
        this.port = activeGrpServerInfos[i].jmxPort();
        break;
      }
    }

    if (!isActiveFound) { throw new Exception("No Active coordinator could be found"); }

    String activeCordinatorIp = getIpAddressOfServer(this.host);
    String ipOfHostnamePassed = getIpAddressOfServer(host);

    if (activeCordinatorIp.equals(ipOfHostnamePassed) && this.port == jmxPort) { return true; }
    return false;
  }

  private ServerGroupInfo[] getServerGroupInfo() throws Exception {
    ServerGroupInfo[] serverGrpInfos = null;
    TCServerInfoMBean mbean = null;
    final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector(userName, host, port);
    final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    mbean = MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
    serverGrpInfos = mbean.getServerGroupInfo();
    jmxConnector.close();
    return serverGrpInfos;
  }

  private boolean isActive(String hostname, int jmxPort) {
    TCServerInfoMBean mbean = null;
    boolean isActive = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = CommandLineBuilder.getJMXConnector(userName, hostname, jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isActive = mbean.isActive();
    } catch (Exception e) {
      return false;
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          // System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }

    return isActive;
  }

  private String getIpAddressOfServer(final String name) throws Exception {
    InetAddress address;
    address = InetAddress.getByName(name);
    if (address.isLoopbackAddress()) {
      address = InetAddress.getLocalHost();
    }
    return address.getHostAddress();
  }
}
