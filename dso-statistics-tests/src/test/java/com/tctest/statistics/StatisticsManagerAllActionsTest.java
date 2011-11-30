/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.test.JMXUtils;
import com.tc.util.UUID;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class StatisticsManagerAllActionsTest extends AbstractStatisticsTransparentTestBase {
  @Override
  protected void duringRunningCluster() throws Exception {
    waitForAllNodesToConnectToGateway(StatisticsManagerAllActionsTestApp.NODE_COUNT + 1);

    JMXConnector jmxc = JMXUtils.getJMXConnector("localhost", getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    StatisticsManagerMBean stat_manager = MBeanServerInvocationHandler
        .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_MANAGER, StatisticsManagerMBean.class, false);
    StatisticsEmitterMBean stat_emitter = MBeanServerInvocationHandler
        .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_EMITTER, StatisticsEmitterMBean.class, false);

    List<StatisticData> data = new ArrayList<StatisticData>();
    CollectingNotificationListener listener = new CollectingNotificationListener(1);
    mbsc.addNotificationListener(StatisticsMBeanNames.STATISTICS_EMITTER, listener, null, data);
    stat_emitter.enable();

    String sessionid = UUID.getUUID().toString();
    stat_manager.createSession(sessionid);

    // register all the supported statistics
    String[] statistics = stat_manager.getSupportedStatistics();
    for (String statistic : statistics) {
      stat_manager.enableStatistic(sessionid, statistic);
      assertTrue(StatisticType.getAllTypes().contains(StatisticType.getType(stat_manager.getStatisticType(statistic))));
    }

    // start capturing
    stat_manager.startCapturing(sessionid);

    // wait for 10 seconds
    Thread.sleep(10000);

    // stop capturing and wait for the last data
    synchronized (listener) {
      stat_manager.stopCapturing(sessionid);
      while (!listener.getShutdown()) {
        listener.wait(2000);
      }
    }

    // disable the notification and detach the listener
    stat_emitter.disable();
    mbsc.removeNotificationListener(StatisticsMBeanNames.STATISTICS_EMITTER, listener);

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
    return StatisticsManagerAllActionsTestApp.class;
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsManagerAllActionsTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}