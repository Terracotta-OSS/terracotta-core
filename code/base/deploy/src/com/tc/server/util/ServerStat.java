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
    if (!connected) return "failed to connect at port " + port;
    return infoBean.getHealthStatus();
  }

  public String toString() {
    String newline = System.getProperty("line.separator");
    StringBuilder sb = new StringBuilder();
    sb.append("- " + host + ":").append(newline);
    sb.append("  health: " + getHealth()).append(newline);
    sb.append("  role: " + getRole()).append(newline);
    sb.append("  state: " + getState()).append(newline);
    sb.append("  jmxport: " + port).append(newline);
    return sb.toString();
  }

  public static void main(String[] args) {
    String usage = "    server-stat.sh -s host1,host2\n" +
                   "    server-stat.sh -s host1:9520,host2:9520\n" +
                   "    server-stat.sh -f /path/to/tc-config.xml\n";
    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(ServerStat.class.getName(), args);

    commandLineBuilder.addOption("s", true, "Terracotta server list (comma separated)", String.class, false, "list");
    commandLineBuilder.addOption("f", true, "Terracotta tc-config file", String.class, false, "file");
    commandLineBuilder.addOption("h", "help", String.class, false);
    commandLineBuilder.setUsageMessage(usage);
    commandLineBuilder.parse();

    if (commandLineBuilder.hasOption('h')) {
      commandLineBuilder.usageAndDie(usage);
    }

    String hostList = commandLineBuilder.getOptionValue('s');
    String configFile = commandLineBuilder.getOptionValue('f');

    if (hostList == null) {
      printStat("localhost:9520");
    } else {
      String[] pairs = hostList.split(",");
      for (String info : pairs) {
        printStat(info);
        System.out.println();
      }
    }
    
    if (configFile != null) {
      //
    }
  }
  
  public static void printStat(String info) {
    String host = "localhost";
    int port = 9520;
    if (info.indexOf(':') > 0) {
      String[] args = info.split(":");
      host = args[0];
      try {
        port = Integer.valueOf(args[1]);
      } catch (NumberFormatException e) {
        System.err.println("Error parsing port number from: " + info + ". Using default port 9520");
        port = 9520;
      }
    }
    ServerStat stat = new ServerStat(host, port);
    System.out.println(stat.toString());
  }
}
