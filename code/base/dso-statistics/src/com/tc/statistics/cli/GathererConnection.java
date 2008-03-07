/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class GathererConnection {
  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 9520;

  private String host = DEFAULT_HOST;
  private int port = DEFAULT_PORT;
  private JMXConnector jmxc;
  private MBeanServerConnection mbsc;
  private StatisticsLocalGathererMBean gatherer;

  public GathererConnection() {
  }

  public StatisticsLocalGathererMBean getGatherer() {
    return gatherer;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void connect() throws IOException {
    jmxc = new JMXConnectorProxy(host, port);

    // create the server connection
    mbsc = jmxc.getMBeanServerConnection();

    // setup the mbeans
    gatherer = (StatisticsLocalGathererMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATHERER, StatisticsLocalGathererMBean.class, true);
  }
}
