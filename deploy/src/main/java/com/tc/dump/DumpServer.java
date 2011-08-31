/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.dump;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class DumpServer {
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  private final String          host;
  private final int             port;
  private final String          username;
  private final String          password;

  public static final String    DEFAULT_HOST  = "localhost";
  public static final int       DEFAULT_PORT  = 9520;

  public static void main(String[] args) throws Exception {

    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(DumpServer.class.getName(), args);

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
      System.err.println("Dumping server at " + host + ":" + port);
      new DumpServer(host, port, username, password).dumpServer();
    } catch (IOException ioe) {
      System.err.println("Unable to connect to host '" + host + "', port " + port
                         + ". Are you sure there is a Terracotta server instance running there?");
    } catch (SecurityException se) {
      System.err.println(se.getMessage());
      commandLineBuilder.usageAndDie();
    }
  }

  private static int parsePort(String portString) {
    int port = -1;
    try {
      port = Integer.parseInt(portString);
    } catch (NumberFormatException e) {
      port = DEFAULT_PORT;
      System.err.println("Invalid port number specified. Using default port '" + port + "'");
    }
    return port;
  }

  public DumpServer(String host, int port) {
    this(host, port, null, null);
  }

  public DumpServer(String host, int port, String userName, String password) {
    this.host = host;
    this.port = port;
    this.username = userName;
    this.password = password;
  }

  public void dumpServer() throws Exception {
    L2DumperMBean mbean = null;
    final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector(username, password, host, port);
    final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    mbean = MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.DUMPER, L2DumperMBean.class, false);
    try {
      mbean.doServerDump();
    } catch (RuntimeException e) {
      // DEV-1168
      consoleLogger.error((e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
    }
  }
}
