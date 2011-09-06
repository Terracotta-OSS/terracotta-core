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
  private final String          username;
  private final String          password;

  public static final String    DEFAULT_HOST  = "localhost";
  public static final int       DEFAULT_PORT  = 9520;

  public static void main(String[] args) throws Exception {

    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(GCRunner.class.getName(), args);

    commandLineBuilder.addOption("n", "hostname", true, "The Terracotta Server instane hostname", String.class, false,
                                 "l2-hostname");
    commandLineBuilder.addOption("p", "jmxport", true, "Terracotta Server instance JMX port", Integer.class, false,
                                 "l2-jmx-port");
    commandLineBuilder.addOption("u", "username", true, "username", String.class, false);
    commandLineBuilder.addOption("w", "password", true, "password", String.class, false);
    commandLineBuilder.addOption("h", "help", String.class, false);

    commandLineBuilder.parse();

    String[] arguments = commandLineBuilder.getArguments();
    if (arguments.length > 2) {
      commandLineBuilder.usageAndDie();
    }

    if (commandLineBuilder.hasOption('h')) {
      commandLineBuilder.usageAndDie();
    }

    String username = null;
    String password = null;
    if (commandLineBuilder.hasOption('u')) {
      username = commandLineBuilder.getOptionValue('u');
      if (commandLineBuilder.hasOption('w')) {
        password = commandLineBuilder.getOptionValue('w');
      } else {
        password = CommandLineBuilder.readPassword();
      }
    }

    String host = commandLineBuilder.getOptionValue('n');
    String portString = commandLineBuilder.getOptionValue('p');
    int port = portString != null ? parsePort(commandLineBuilder.getOptionValue('p')) : DEFAULT_PORT;

    if (arguments.length == 1) {
      host = DEFAULT_HOST;
      port = parsePort(arguments[0]);
    } else if (arguments.length == 2) {
      host = arguments[0];
      port = parsePort(arguments[1]);
    }

    host = host == null ? DEFAULT_HOST : host;

    try {
      consoleLogger.info("Invoking DGC on " + host + ":" + port);
      new GCRunner(host, port, username, password).runGC();
    } catch (IOException ioe) {
      consoleLogger.error("Unable to connect to host '" + host + "', port " + port
                          + ". Are you sure there is a Terracotta server instance running there?");
    } catch (SecurityException se) {
      consoleLogger.error(se.getMessage());
      commandLineBuilder.usageAndDie();
    }
  }

  private static int parsePort(String portString) {
    int port = -1;
    try {
      port = Integer.parseInt(portString);
    } catch (NumberFormatException e) {
      port = DEFAULT_PORT;
      consoleLogger.warn("Invalid port number specified. Using default port '" + port + "'");
    }
    return port;
  }

  public GCRunner(String host, int port) {
    this(host, port, null, null);
  }

  public GCRunner(String host, int port, String userName, String password) {
    this.host = host;
    this.port = port;
    this.username = userName;
    this.password = password;
  }

  public void runGC() throws Exception {
    if (!setActiveCoordinatorJmxPortAndHost(host, port)) {
      consoleLogger.info("DGC can only be called on server " + host + " with JMX port " + port
                         + ". So the request is being redirected.");
    }

    ObjectManagementMonitorMBean mbean = null;
    final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector(username, password, host, port);
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
    for (ServerGroupInfo serverGrpInfo : serverGrpInfos) {
      if (serverGrpInfo.isCoordinator()) {
        activeGrpServerInfos = serverGrpInfo.members();
      }
    }

    boolean isActiveFound = false;
    for (L2Info activeGrpServerInfo : activeGrpServerInfos) {
      if (isActive(activeGrpServerInfo.host(), activeGrpServerInfo.jmxPort())) {
        isActiveFound = true;
        this.host = activeGrpServerInfo.host();
        this.port = activeGrpServerInfo.jmxPort();
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
    final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector(username, password, host, port);
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
      jmxConnector = CommandLineBuilder.getJMXConnector(username, password, hostname, jmxPort);
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
