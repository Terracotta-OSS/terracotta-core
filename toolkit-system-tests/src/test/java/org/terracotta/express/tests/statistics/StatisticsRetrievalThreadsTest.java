/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.statistics;

import org.terracotta.test.util.JMXUtils;

import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.retrieval.impl.StatisticsRetrieverImpl;
import com.tc.test.config.model.TestConfig;
import com.tc.util.UUID;
import com.tc.util.runtime.ThreadDumpUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

import junit.framework.Assert;

public class StatisticsRetrievalThreadsTest extends AbstractStatisticsTestBase {
  public StatisticsRetrievalThreadsTest(TestConfig testConfig) {
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
    System.out.println("Statistics Gateway Enabled");

    String sessionid = UUID.getUUID().toString();
    for (int i = 0; i < 5; i++) {
      stat_gateway.createSession(sessionid + i);

      // start capturing
      stat_gateway.startCapturing(sessionid + i);
    }
    System.out.println("Statistics gateway capturing started going to sleep for 30 sec");

    TimeUnit.SECONDS.sleep(30);

    for (int i = 0; i < 5; i++) {
      // stop capturing and wait for the last data
      synchronized (listener) {
        stat_gateway.stopCapturing(sessionid + i);
        while (!listener.getShutdown()) {
          System.out.println("Going to wait for 2 sec for listener to shutdown");
          listener.wait(2000);
        }
        listener.reset();
      }
    }
    System.out.println("Statistics gateway capturing stopped.");

    String thread_dump = ThreadDumpUtil.getThreadDump();
    Assert.assertFalse(thread_dump.contains(StatisticsRetrieverImpl.TIMER_NAME));

    // disable the notification and detach the listener
    stat_gateway.disable();
    mbsc.removeNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener);
  }

}
