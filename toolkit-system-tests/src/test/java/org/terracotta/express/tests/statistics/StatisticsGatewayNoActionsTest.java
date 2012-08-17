/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.statistics;

import org.terracotta.test.util.JMXUtils;

import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.test.config.model.TestConfig;
import com.tc.util.UUID;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class StatisticsGatewayNoActionsTest extends AbstractStatisticsTestBase {
  public StatisticsGatewayNoActionsTest(TestConfig testConfig) {
    super(testConfig);
  }

  @Override
  protected void duringRunningCluster() throws Exception {
    JMXConnector jmxc = JMXUtils.getJMXConnector("localhost", getGroupData(0).getJmxPort(0));
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    StatisticsGatewayMBean stat_gateway = MBeanServerInvocationHandler
        .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

    waitForAllNodesToConnectToGateway(stat_gateway, StatisticsGatewayTestClient.NODE_COUNT + 1);

    List<StatisticData> data = new ArrayList<StatisticData>();
    CollectingNotificationListener listener = new CollectingNotificationListener(
                                                                                 StatisticsGatewayTestClient.NODE_COUNT + 1);
    mbsc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener, null, data);
    stat_gateway.enable();

    String sessionid = UUID.getUUID().toString();
    stat_gateway.createSession(sessionid);

    // register all the supported statistics
    String[] statistics = stat_gateway.getSupportedStatistics();
    for (String statistic : statistics) {
      stat_gateway.enableStatistic(sessionid, statistic);
    }

    // remove all statistics
    stat_gateway.disableAllStatistics(sessionid);

    // start capturing
    stat_gateway.startCapturing(sessionid);

    // wait for 10 seconds
    Thread.sleep(10000);

    // stop capturing and wait for the last data
    synchronized (listener) {
      stat_gateway.stopCapturing(sessionid);
      while (!listener.getShutdown()) {
        listener.wait(2000);
      }
    }

    // disable the notification and detach the listener
    stat_gateway.disable();
    mbsc.removeNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener);

    // check the data
    assertEquals((StatisticsGatewayTestClient.NODE_COUNT + 1) * 2, data.size());
    assertEquals(SRAStartupTimestamp.ACTION_NAME, data.get(0).getName());
    assertEquals(SRAShutdownTimestamp.ACTION_NAME, data.get(data.size() - 1).getName());
  }

}
