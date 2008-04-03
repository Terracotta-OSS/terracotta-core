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
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

public class StatisticsGatewayCaptureStatisticTest extends TransparentTestBase {
  protected void duringRunningCluster() throws Exception {
    int total_node_count = StatisticsGatewayNoActionsTestApp.NODE_COUNT + 1;

    JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    StatisticsGatewayMBean stat_gateway = (StatisticsGatewayMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

    List<StatisticData> data = new ArrayList<StatisticData>();
    CollectingNotificationListener listener = new CollectingNotificationListener(total_node_count);
    mbsc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener, null, data);
    stat_gateway.enable();

    String sessionid = UUID.getUUID().toString();
    stat_gateway.createSession(sessionid);

    // start capturing
    stat_gateway.startCapturing(sessionid);

    // wait for 3 seconds
    Thread.sleep(3000);

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

    // wait for 3 seconds
    Thread.sleep(3000);

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
    assertEquals(total_node_count * 4, data.size());
    for (int i = 0; i < total_node_count; i++) {
      assertEquals(SRAStartupTimestamp.ACTION_NAME, data.get((total_node_count * 0) + i).getName());
      assertEquals(SRASystemProperties.ACTION_NAME, data.get((total_node_count * 1) + i).getName());
      assertEquals(SRASystemProperties.ACTION_NAME, data.get((total_node_count * 2) + i).getName());
      assertEquals(SRAShutdownTimestamp.ACTION_NAME, data.get((total_node_count * 3) + i).getName());
    }
  }

  protected Class getApplicationClass() {
    return StatisticsManagerNoActionsTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsGatewayNoActionsTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}