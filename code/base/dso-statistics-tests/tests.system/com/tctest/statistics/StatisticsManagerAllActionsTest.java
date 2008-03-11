/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvictRequest;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvicted;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.statistics.retrieval.actions.SRAThreadDump;
import com.tc.util.UUID;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

public class StatisticsManagerAllActionsTest extends TransparentTestBase {
  protected void duringRunningCluster() throws Exception {
    JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    StatisticsManagerMBean stat_manager = (StatisticsManagerMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_MANAGER, StatisticsManagerMBean.class, false);
    StatisticsEmitterMBean stat_emitter = (StatisticsEmitterMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_EMITTER, StatisticsEmitterMBean.class, false);

    List<StatisticData> data = new ArrayList<StatisticData>();
    CollectingNotificationListener listener = new CollectingNotificationListener(1);
    mbsc.addNotificationListener(StatisticsMBeanNames.STATISTICS_EMITTER, listener, null, data);
    stat_emitter.enable();

    String sessionid = UUID.getUUID().toString();
    stat_manager.createSession(sessionid);

    // register all the supported statistics
    int non_triggered_actions = 0;
    String[] statistics = stat_manager.getSupportedStatistics();
    for (String statistic : statistics) {
      stat_manager.enableStatistic(sessionid, statistic);
      if (!SRAThreadDump.ACTION_NAME.equals(statistic) &&
          !SRACacheObjectsEvicted.ACTION_NAME.equals(statistic) &&
          !SRACacheObjectsEvictRequest.ACTION_NAME.equals(statistic)) {
        non_triggered_actions++;
      }
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
    Set<String> received_data_names = new HashSet<String>();
    for (int i = 1; i < data.size() - 1; i++) {
      StatisticData stat_data = data.get(i);
      if (!SRAStartupTimestamp.ACTION_NAME.equals(stat_data.getName()) &&
          !SRAShutdownTimestamp.ACTION_NAME.equals(stat_data.getName())) {
        received_data_names.add(stat_data.getName());
      }
    }
    // check that there's at least one data element name per registered non-triggered statistic
    assertTrue(received_data_names.size() > non_triggered_actions);
  }

  protected Class getApplicationClass() {
    return StatisticsManagerAllActionsTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsManagerAllActionsTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}