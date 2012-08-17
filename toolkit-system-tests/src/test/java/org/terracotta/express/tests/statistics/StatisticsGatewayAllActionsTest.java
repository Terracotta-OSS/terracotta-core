/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.statistics;

import org.terracotta.test.util.JMXUtils;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
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

public class StatisticsGatewayAllActionsTest extends AbstractStatisticsTestBase {

  public StatisticsGatewayAllActionsTest(TestConfig testConfig) {
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
    System.out.println("Statistics gateway Session created.");

    // register all the supported statistics
    String[] statistics = stat_gateway.getSupportedStatistics();
    for (String statistic : statistics) {
      stat_gateway.enableStatistic(sessionid, statistic);
      assertTrue(StatisticType.getAllTypes().contains(StatisticType.getType(stat_gateway.getStatisticType(statistic))));
    }

    // start capturing
    stat_gateway.startCapturing(sessionid);
    System.out.println("Statistics gateway Started Capturing data.");

    // wait for 10 seconds
    Thread.sleep(10000);

    // stop capturing and wait for the last data
    synchronized (listener) {
      stat_gateway.stopCapturing(sessionid);
      while (!listener.getShutdown()) {
        System.out.println("Listener not shutdown waiting for listener to shutdown.");
        listener.wait(2000);
      }
    }
    System.out.println("Statistics gateway Stopped Capturing data.");

    // disable the notification and detach the listener
    stat_gateway.disable();
    mbsc.removeNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener);
    System.out.println("Statistics gateway disabled and removed listner.");
    // check the data
    assertTrue(data.size() > 2);
    assertEquals(SRAStartupTimestamp.ACTION_NAME, data.get(0).getName());
    assertEquals(SRAShutdownTimestamp.ACTION_NAME, data.get(data.size() - 1).getName());
    // we can't check the statistics that we've received since there are
    // statistics that do not have data until there are some transaction
    // between the L1 and L2.
    // e.g. SRAMessages, SRAL2FaultsFromDisk, SRADistributedGC
    System.out.println("Statistics gateway test PASS");
  }

}
