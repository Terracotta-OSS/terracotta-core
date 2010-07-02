/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server.util;

import org.apache.xmlbeans.XmlException;

import com.tc.cli.CommandLineBuilder;
import com.tc.config.Loader;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.terracottatech.config.Server;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class ServerStat {
  private static final String   UNKNOWN          = "unknown";
  private static final String   NEWLINE          = System.getProperty("line.separator");
  private static final int      DEFAULT_JMX_PORT = 9520;

  private final String          host;
  private final int             port;
  private final String          username;
  private final String          password;
  private JMXConnector          jmxProxy;
  private MBeanServerConnection mbsc;

  private TCServerInfoMBean     infoBean;
  private boolean               connected;
  private String                errorMessage     = "";

  public ServerStat(String username, String password, String host, int port) {
    this.username = username;
    this.password = password;
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
    jmxProxy = CommandLineBuilder.getJMXConnector(username, password, host, port);
    try {
      mbsc = jmxProxy.getMBeanServerConnection();
      infoBean = (TCServerInfoMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.TC_SERVER_INFO,
                                                                                   TCServerInfoMBean.class, false);
      connected = true;
    } catch (Exception e) {
      String rootCauseMessage = e.getMessage() != null ? e.getMessage() : e.getCause().getMessage();
      errorMessage = "Failed to connect to " + host + ":" + port + ". "
                     + (rootCauseMessage != null ? rootCauseMessage : "");
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

    commandLineBuilder.addOption("s", true, "Terracotta server instance list (comma separated)", String.class, false,
                                 "list");
    commandLineBuilder.addOption("f", true, "Terracotta tc-config file", String.class, false, "file");
    commandLineBuilder.addOption("h", "help", String.class, false);
    commandLineBuilder.addOption("u", "username", true, "username", String.class, false);
    commandLineBuilder.addOption("w", "password", true, "password", String.class, false);
    commandLineBuilder.setUsageMessage(usage);
    commandLineBuilder.parse();

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

    String hostList = commandLineBuilder.getOptionValue('s');
    String configFile = commandLineBuilder.getOptionValue('f');

    try {
      if (configFile != null) {
        handleConfigFile(username, password, configFile);
      } else {
        handleList(username, password, hostList);
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private static void handleConfigFile(String username, String password, String configFile) {
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
      int jmxPort = servers[i].getJmxPort().getIntValue() == 0 ? DEFAULT_JMX_PORT : servers[i].getJmxPort().getIntValue();
      ServerStat stat = new ServerStat(username, password, host, jmxPort);
      System.out.println(stat.toString());
    }
  }

  private static void handleList(String username, String password, String hostList) {
    if (hostList == null) {
      printStat(username, password, "localhost:9520");
    } else {
      String[] pairs = hostList.split(",");
      for (String info : pairs) {
        printStat(username, password, info);
        System.out.println();
      }
    }
  }

  // info = host | host:port
  private static void printStat(String username, String password, String info) {
    String host = info;
    int port = DEFAULT_JMX_PORT;
    if (info.indexOf(':') > 0) {
      String[] args = info.split(":");
      host = args[0];
      try {
        port = Integer.valueOf(args[1]);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Failed to parse jmxport: " + info);
      }
    }
    ServerStat stat = new ServerStat(username, password, host, port);
    System.out.println(stat.toString());
  }
}
