/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server.util;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.management.TerracottaManagement;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public class ClusterDumper {

  private final String       host;
  private final int          port;
  private final String       username;
  private final String       password;

  public static final String DEFAULT_HOST = "localhost";
  public static final int    DEFAULT_PORT = 9520;

  public static void main(String[] args) throws Exception {
    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(ClusterDumper.class.getName(), args);

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
      System.out.println("Taking dumps by connecting " + host + ":" + port);
      new ClusterDumper(host, port, username, password).takeDump();
    } catch (IOException ioe) {
      System.out.println("Unable to connect to host '" + host + "', port " + port
                         + ". Are you sure there is a Terracotta server instance running there?");
    } catch (SecurityException se) {
      System.out.println(se.getMessage());
      commandLineBuilder.usageAndDie();
    }
  }

  private static int parsePort(String portString) {
    int port = -1;
    try {
      port = Integer.parseInt(portString);
    } catch (NumberFormatException e) {
      port = DEFAULT_PORT;
      System.out.println("Invalid port number specified. Using default port '" + port + "'");
    }
    return port;
  }

  public ClusterDumper(String host, int port) {
    this(host, port, null, null);
  }

  public ClusterDumper(String host, int port, String userName, String password) {
    this.host = host;
    this.port = port;
    this.username = userName;
    this.password = password;
  }

  public void takeDump() throws Exception {
    ServerGroupInfo[] serverGrpInfos = getServerGroupInfo();

    doServerDumps(serverGrpInfos);
    findActiveAndDumpClients(serverGrpInfos);
    System.out.println("All dumps taken. Exiting!!");
  }

  private void doServerDumps(ServerGroupInfo[] serverGrpInfos) {
    for (ServerGroupInfo serverGrpInfo : serverGrpInfos) {
      L2Info[] members = serverGrpInfo.members();
      for (L2Info member : members) {
        L2DumperMBean mbean = null;
        JMXConnector jmxConnector = null;

        try {
          String hostName = member.host();
          int jmxPort = member.jmxPort();
          System.out.println("trying to take server dump for " + hostName + ":" + jmxPort);
          jmxConnector = CommandLineBuilder.getJMXConnector(username, password, hostName, jmxPort);
          final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
          mbean = MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.DUMPER, L2DumperMBean.class, false);
          mbean.doServerDump();
          System.out.println("server dump taken for " + hostName + ":" + jmxPort);
        } catch (Exception e) {
          System.out.println((e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
        } finally {
          if (jmxConnector != null) {
            try {
              jmxConnector.close();
            } catch (Exception e) {
              // System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
            }
          }
        }
      }
    }
  }

  private void findActiveAndDumpClients(ServerGroupInfo[] serverGrpInfos) {
    L2Info[] l2Infos = null;
    for (ServerGroupInfo serverGrpInfo : serverGrpInfos) {
      if (serverGrpInfo.isCoordinator()) {
        l2Infos = serverGrpInfo.members();
        break;
      }
    }

    if (l2Infos == null) {
      System.out.println("Active coordinator group not found, clients dump are not taken.");
      return;
    }

    for (L2Info l2Info : l2Infos) {
      String hostName = l2Info.host();
      int jmxPort = l2Info.jmxPort();
      if (isActive(hostName, jmxPort)) {
        doClientsDump(hostName, jmxPort);
      }
    }
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

  private void doClientsDump(String hostName, int jmxPort) {
    System.out.println("trying to take client dumps by connecting " + hostName + ":" + jmxPort);
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = CommandLineBuilder.getJMXConnector(username, password, hostName, jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      Set allL1DumperMBeans;
      allL1DumperMBeans = TerracottaManagement.getAllL1DumperMBeans(mbs);

      for (Iterator iterator = allL1DumperMBeans.iterator(); iterator.hasNext();) {
        ObjectName l1DumperBean = (ObjectName) iterator.next();
        mbs.invoke(l1DumperBean, "doClientDump", new Object[] {}, new String[] {});
        System.out.println("dumping client " + l1DumperBean.getCanonicalName());
      }
    } catch (Exception e) {
      System.out.println((e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          // System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }
  }
}
