/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.statistics.retrieval.actions.SRASystemProperties;
import com.tc.util.UUID;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

public class StatisticsGatewayCaptureStatisticTest extends AbstractStatisticsTransparentTestBase {
  @Override
  protected void duringRunningCluster() throws Exception {
    int total_node_count = StatisticsGatewayNoActionsTestApp.NODE_COUNT + 1;

    JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    StatisticsGatewayMBean stat_gateway = (StatisticsGatewayMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

    waitForAllNodesToConnectToGateway(stat_gateway, total_node_count);

    List<StatisticData> data = new ArrayList<StatisticData>();
    CollectingNotificationListener listener = new CollectingNotificationListener(total_node_count);
    mbsc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener, null, data);
    stat_gateway.enable();

    String sessionid = UUID.getUUID().toString();
    stat_gateway.createSession(sessionid);

    // start capturing
    stat_gateway.startCapturing(sessionid);

    // wait for 10 seconds
    Thread.sleep(10000);

    StatisticData[] data1 = stat_gateway.captureStatistic(sessionid, SRASystemProperties.ACTION_NAME);
    assertEquals(total_node_count, data1.length);
    for (int i = 0; i < total_node_count; i++) {
      assertEquals(SRASystemProperties.ACTION_NAME, data1[i].getName());
    }
    StatisticData[] data2 = stat_gateway.captureStatistic(sessionid, SRASystemProperties.ACTION_NAME);
    assertEquals(total_node_count, data2.length);
    for (int i = 0; i < total_node_count; i++) {
      assertEquals(SRASystemProperties.ACTION_NAME, data2[i].getName());
    }

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

    assertEquals(total_node_count * 4, data.size());

    // aggregate the data
    Map dataMap = new HashMap();
    for (Object element : data) {
      StatisticData dataEntry = (StatisticData)element;
      List dataNameAggregation = (List)dataMap.get(dataEntry.getName());
      if (null == dataNameAggregation) {
        dataNameAggregation = new ArrayList();
        dataMap.put(dataEntry.getName(), dataNameAggregation);
      }
      dataNameAggregation.add(dataEntry);
    }

    // check the data
    assertTrue(dataMap.containsKey(SRAStartupTimestamp.ACTION_NAME));
    assertTrue(dataMap.containsKey(SRASystemProperties.ACTION_NAME));
    assertTrue(dataMap.containsKey(SRAShutdownTimestamp.ACTION_NAME));
    assertEquals(total_node_count, ((List)dataMap.get(SRAStartupTimestamp.ACTION_NAME)).size());
    assertEquals(total_node_count * 2, ((List)dataMap.get(SRASystemProperties.ACTION_NAME)).size());
    assertEquals(total_node_count, ((List)dataMap.get(SRAShutdownTimestamp.ACTION_NAME)).size());
  }

  @Override
  protected Class getApplicationClass() {
    return StatisticsGatewayCaptureStatisticTestApp.class;
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsGatewayCaptureStatisticTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}