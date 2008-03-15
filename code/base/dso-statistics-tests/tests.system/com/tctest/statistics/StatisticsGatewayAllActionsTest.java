/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvictRequest;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvicted;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.statistics.retrieval.actions.SRAThreadDump;
import com.tc.util.UUID;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

public class StatisticsGatewayAllActionsTest extends TransparentTestBase {
  protected void duringRunningCluster() throws Exception {
    JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    StatisticsGatewayMBean stat_gateway = (StatisticsGatewayMBean)MBeanServerInvocationHandler
      .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

    List<StatisticData> data = new ArrayList<StatisticData>();
    CollectingNotificationListener listener = new CollectingNotificationListener(StatisticsGatewayAllActionsTestApp.NODE_COUNT + 1);
    mbsc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener, null, data);
    stat_gateway.enable();

    String sessionid = UUID.getUUID().toString();
    stat_gateway.createSession(sessionid);

    // register all the supported statistics
    int non_triggered_actions = 0;
    String[] statistics = stat_gateway.getSupportedStatistics();
    Collection<String> nonTriggeredActions = new TreeSet<String>();
    for (String statistic : statistics) {
      stat_gateway.enableStatistic(sessionid, statistic);
      if (!SRAThreadDump.ACTION_NAME.equals(statistic) &&
          !SRACacheObjectsEvicted.ACTION_NAME.equals(statistic) &&
          !SRACacheObjectsEvictRequest.ACTION_NAME.equals(statistic)
        ) {
        non_triggered_actions++;
        nonTriggeredActions.add(statistic);
      }
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
    Set<String> received_data_names = new TreeSet<String>();
    for (int i = 1; i < data.size() - 1; i++) {
      StatisticData stat_data = data.get(i);
      if (!SRAStartupTimestamp.ACTION_NAME.equals(stat_data.getName()) &&
          !SRAShutdownTimestamp.ACTION_NAME.equals(stat_data.getName())) {
        received_data_names.add(stat_data.getName());
      }
    }
    System.out.println("Received actions size=" + received_data_names.size() +
                       " Non-triggered size=" + nonTriggeredActions.size());
    System.out.println("Received actions: " + received_data_names);
    System.out.println("Non-triggered actions: " + nonTriggeredActions);
    // check that there's at least one data element name per registered statistic
    // this assert is not true since there are statistics that do not have data
    // until there are some transaction between the L1 and L2.
    // e.g. SRAMessages, SRAL2FaultsFromDisk, SRADistributedGC
    //assertTrue(received_data_names.size() >= non_triggered_actions);
  }

  protected Class getApplicationClass() {
    return StatisticsGatewayAllActionsTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsGatewayAllActionsTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}