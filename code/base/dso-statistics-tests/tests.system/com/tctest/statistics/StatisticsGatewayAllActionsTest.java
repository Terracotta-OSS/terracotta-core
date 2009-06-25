/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.util.UUID;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

public class StatisticsGatewayAllActionsTest extends AbstractStatisticsTransparentTestBase {
  @Override
  protected void duringRunningCluster() throws Exception {
    JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    StatisticsGatewayMBean stat_gateway = (StatisticsGatewayMBean)MBeanServerInvocationHandler
      .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

    waitForAllNodesToConnectToGateway(stat_gateway, StatisticsGatewayAllActionsTestApp.NODE_COUNT+1);

    List<StatisticData> data = new ArrayList<StatisticData>();
    CollectingNotificationListener listener = new CollectingNotificationListener(StatisticsGatewayAllActionsTestApp.NODE_COUNT + 1);
    mbsc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener, null, data);
    stat_gateway.enable();

    String sessionid = UUID.getUUID().toString();
    stat_gateway.createSession(sessionid);

    // register all the supported statistics
    String[] statistics = stat_gateway.getSupportedStatistics();
    for (String statistic : statistics) {
      stat_gateway.enableStatistic(sessionid, statistic);
      assertTrue(StatisticType.getAllTypes().contains(StatisticType.getType(stat_gateway.getStatisticType(statistic))));
    }


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
    assertTrue(data.size() > 2);
    assertEquals(SRAStartupTimestamp.ACTION_NAME, data.get(0).getName());
    assertEquals(SRAShutdownTimestamp.ACTION_NAME, data.get(data.size() - 1).getName());
    // we can't check the statistics that we've received since there are
    // statistics that do not have data until there are some transaction
    // between the L1 and L2.
    // e.g. SRAMessages, SRAL2FaultsFromDisk, SRADistributedGC
  }

  @Override
  protected Class getApplicationClass() {
    return StatisticsGatewayAllActionsTestApp.class;
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsGatewayAllActionsTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}