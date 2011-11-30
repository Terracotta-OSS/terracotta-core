/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.cli;

import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class GathererConnection {
  public static final String           DEFAULT_HOST = "localhost";
  public static final int              DEFAULT_PORT = 9520;

  private String                       host         = DEFAULT_HOST;
  private int                          port         = DEFAULT_PORT;
  private String                       username;
  private String                       password;
  private StatisticsLocalGathererMBean gatherer;
  private TCServerInfoMBean            info;

  public GathererConnection() {
    //
  }

  public StatisticsLocalGathererMBean getGatherer() {
    return gatherer;
  }

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(final int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public int getDSOListenPort() {
    return info.getDSOListenPort();
  }

  public void connect() throws IOException {
    Map env = new HashMap();
    if (username != null && password != null) {
      String[] creds = { username, password };
      env.put("jmx.remote.credentials", creds);
    }
    JMXConnector jmxc = new JMXConnectorProxy(host, port, env);

    // create the server connection
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    // setup the mbeans
    gatherer = MBeanServerInvocationHandler.newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATHERER,
                                                             StatisticsLocalGathererMBean.class, true);
    info = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class,
                                                         false);
  }
}
