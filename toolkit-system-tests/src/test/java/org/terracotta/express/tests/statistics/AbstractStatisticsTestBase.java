/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.statistics;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.JMXUtils;

import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.test.config.model.TestConfig;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public abstract class AbstractStatisticsTestBase extends AbstractToolkitTestBase {

  public AbstractStatisticsTestBase(TestConfig testConfig, Class<? extends ClientBase>[] c) {
    super(testConfig, c);
    testConfig.getClientConfig().getBytemanConfig().setScript("/mnk-3920.btm");
  }

  public AbstractStatisticsTestBase(TestConfig testConfig) {
    super(testConfig, StatisticsGatewayTestClient.class, StatisticsGatewayTestClient.class);
    testConfig.getClientConfig().getBytemanConfig().setScript("/mnk-3920.btm");
  }

  protected void waitForAllNodesToConnectToGateway(final int nodeCount) throws IOException, InterruptedException {
    final JMXConnector jmxc = JMXUtils.getJMXConnector("localhost", getGroupData(0).getJmxPort(0));
    final MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    final StatisticsGatewayMBean stat_gateway = MBeanServerInvocationHandler
        .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

    waitForAllNodesToConnectToGateway(stat_gateway, nodeCount);
  }

  protected void waitForAllNodesToConnectToGateway(final StatisticsGatewayMBean statGateway, final int nodeCount)
      throws InterruptedException {
    int currentNodeCount;
    Thread.sleep(2000);
    while ((currentNodeCount = statGateway.getConnectedAgentChannelIDs().length) < nodeCount) {
      Thread.sleep(500);
      System.out.println("Currently " + currentNodeCount + " nodes connected to gateway, waiting for " + nodeCount);
    }
    System.out.println("Currently " + currentNodeCount + " nodes connected to gateway");
  }
}
