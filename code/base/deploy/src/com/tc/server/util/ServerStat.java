/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server.util;

import org.apache.xmlbeans.XmlException;

import com.tc.cli.CommandLineBuilder;
import com.tc.config.Loader;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.terracottatech.config.Server;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

public class ServerStat {
  private static final String   UNKNOWN      = "unknown";
  private static final String   NEWLINE      = System.getProperty("line.separator");

  public String                 host;
  public int                    port;
  private JMXConnectorProxy     jmxProxy;
  private MBeanServerConnection mbsc;

  private TCServerInfoMBean     infoBean;
  private boolean               connected;
  private String                errorMessage = "";

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
    } catch (Exception e) {
      errorMessage = "Failed to connect to " + host + ":" + port;
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
    if (!connected) return "FAILED";
    return infoBean.getHealthStatus();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(host + ".health: " + getHealth()).append(NEWLINE);
    sb.append(host + ".role: " + getRole()).append(NEWLINE);
    sb.append(host + ".state: " + getState()).append(NEWLINE);
    sb.append(host + ".jmxport: " + port).append(NEWLINE);
    if (!connected) {
      sb.append(host + ".error: " + errorMessage).append(NEWLINE);
    }
    return sb.toString();
  }

  public static void main(String[] args) {
    String usage = " server-stat -s host1,host2" + NEWLINE + "       server-stat -s host1:9520,host2:9520" + NEWLINE
                   + "       server-stat -f /path/to/tc-config.xml" + NEWLINE;

    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(ServerStat.class.getName(), args);

    commandLineBuilder.addOption("s", true, "Terracotta server instance list (comma separated)", String.class, false, "list");
    commandLineBuilder.addOption("f", true, "Terracotta tc-config file", String.class, false, "file");
    commandLineBuilder.addOption("h", "help", String.class, false);
    commandLineBuilder.setUsageMessage(usage);
    commandLineBuilder.parse();

    if (commandLineBuilder.hasOption('h')) {
      commandLineBuilder.usageAndDie(usage);
    }

    String hostList = commandLineBuilder.getOptionValue('s');
    String configFile = commandLineBuilder.getOptionValue('f');

    try {
      if (configFile != null) {
        handleConfigFile(configFile);
      } else {
        handleList(hostList);
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private static void handleConfigFile(String configFile) {
    TcConfigDocument tcConfigDocument = null;
    try {
      tcConfigDocument = new Loader().parse(new File(configFile));
    } catch (IOException e) {
      throw new RuntimeException("Error reading " + configFile + ": " + e.getMessage());
    } catch (XmlException e) {
      throw new RuntimeException("Error parsing " + configFile + ": " + e.getMessage());
    }
    TcConfig tcConfig = tcConfigDocument.getTcConfig();
    Server[] servers = tcConfig.getServers().getServerArray();
    for (int i = 0; i < servers.length; i++) {
      String host = servers[i].getHost();
      if ("%h".equals(host)) host = ParameterSubstituter.getHostname();
      if ("%i".equals(host)) host = ParameterSubstituter.getIpAddress();
      ServerStat stat = new ServerStat(host, servers[i].getJmxPort());
      System.out.println(stat.toString());
    }
  }

  private static void handleList(String hostList) {
    if (hostList == null) {
      printStat("localhost:9520");
    } else {
      String[] pairs = hostList.split(",");
      for (String info : pairs) {
        printStat(info);
        System.out.println();
      }
    }
  }

  // info = host | host:port
  private static void printStat(String info) {
    String host = info;
    int port = 9520;
    if (info.indexOf(':') > 0) {
      String[] args = info.split(":");
      host = args[0];
      try {
        port = Integer.valueOf(args[1]);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Failed to parse jmxport: " + info);
      }
    }
    ServerStat stat = new ServerStat(host, port);
    System.out.println(stat.toString());
  }
}
