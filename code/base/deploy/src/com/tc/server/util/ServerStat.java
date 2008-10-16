/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server.util;

import com.tc.cli.CommandLineBuilder;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

public class ServerStat {
  private static final String   UNKNOWN = "unknown";
  public String                 host;
  public int                    port;
  private JMXConnectorProxy     jmxProxy;
  private MBeanServerConnection mbsc;

  private TCServerInfoMBean     infoBean;
  private boolean               connected;

  public ServerStat(String host, int port) {
    this.host = host;
    this.port = port;
    connect();
  }

  private void connect() {
    if (jmxProxy != null) {
      try {
        jmxProxy.close();
      } catch (IOException e) {
        // ignore
      }
    }
    jmxProxy = new JMXConnectorProxy(host, port);
    try {
      mbsc = jmxProxy.getMBeanServerConnection();
      infoBean = (TCServerInfoMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.TC_SERVER_INFO,
                                                                                   TCServerInfoMBean.class, false);
      connected = true;
    } catch (IOException e) {
      connected = false;
    }
  }

  public String getState() {
    if (!connected) return UNKNOWN;
    return infoBean.getState();
  }

  public String getRole() {
    if (!connected) return UNKNOWN;
    return infoBean.isActive() ? "ACTIVE" : "PASSIVE";
  }

  public String getHealth() {
    if (!connected) return UNKNOWN;
    return infoBean.getHealthStatus();
  }

  public String toString() {
    String newline = System.getProperty("line.separator");
    StringBuilder sb = new StringBuilder();
    sb.append("- " + host + ":").append(newline);
    sb.append("  health: " + getHealth()).append(newline);
    sb.append("  role: " + getRole()).append(newline);
    sb.append("  state: " + getState()).append(newline);
    return sb.toString();
  }

  public static void main(String[] args) {
    String host = "localhost";
    int port = 9520;

    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(ServerStat.class.getName(), args);

    commandLineBuilder.addOption("n", "hostname", true, "Terracotta Server hostname", String.class, false, "hostname");
    commandLineBuilder.addOption("p", "jmxport", true, "Terracotta Server JMX port", Integer.class, false, "jmxport");
    commandLineBuilder.addOption("h", "help", String.class, false);
    commandLineBuilder.setUsageMessage("server-stat.bat/server-stat.sh");
    commandLineBuilder.parse();

    if (commandLineBuilder.hasOption('h')) {
      commandLineBuilder.usageAndDie("server-stat.bat/server-stat.sh");
    }

    String hostValue = commandLineBuilder.getOptionValue('n');
    String portValue = commandLineBuilder.getOptionValue('p');

    if (hostValue != null) host = hostValue;

    if (portValue != null) {
      try {
        port = Integer.parseInt(portValue);
      } catch (NumberFormatException e) {
        System.err.println("Invalid port number specified. Using default port '" + port + "'");
      }
    }

    ServerStat stat = new ServerStat(host, port);
    System.out.println(stat.toString());

  }
}
