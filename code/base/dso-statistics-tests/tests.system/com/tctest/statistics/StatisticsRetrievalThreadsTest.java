/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.retrieval.impl.StatisticsRetrieverImpl;
import com.tc.util.UUID;
import com.tc.util.runtime.ThreadDumpUtil;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

import junit.framework.Assert;

public class StatisticsRetrievalThreadsTest extends AbstractStatisticsTransparentTestBase {
  @Override
  protected void duringRunningCluster() throws Exception {
    JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    StatisticsGatewayMBean stat_gateway = (StatisticsGatewayMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

    waitForAllNodesToConnectToGateway(stat_gateway, StatisticsRetrievalThreadsTestApp.NODE_COUNT+1);

    List<StatisticData> data = new ArrayList<StatisticData>();
    CollectingNotificationListener listener = new CollectingNotificationListener(StatisticsRetrievalThreadsTestApp.NODE_COUNT + 1);
    mbsc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener, null, data);
    stat_gateway.enable();

    String sessionid = UUID.getUUID().toString();
    for (int i = 0; i < 5; i++) {
      stat_gateway.createSession(sessionid + i);

      // start capturing
      stat_gateway.startCapturing(sessionid + i);
    }
    
    Thread.sleep(120000);
    
    for (int i = 0; i < 5; i++) {
      // stop capturing and wait for the last data
      synchronized (listener) {
        stat_gateway.stopCapturing(sessionid + i);
        while (!listener.getShutdown()) {
          listener.wait(2000);
        }
        listener.reset();
      }
    }

    String thread_dump = ThreadDumpUtil.getThreadDump();
    Assert.assertFalse(thread_dump.contains(StatisticsRetrieverImpl.TIMER_NAME));

    // disable the notification and detach the listener
    stat_gateway.disable();
    mbsc.removeNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener);
  }

  @Override
  protected Class getApplicationClass() {
    return StatisticsRetrievalThreadsTestApp.class;
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsRetrievalThreadsTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}