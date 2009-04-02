/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tctest.TransparentTestBase;
import com.tctest.runner.PostAction;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

public abstract class AbstractStatisticsTransparentTestBase extends TransparentTestBase {

  protected static class BaseStatisticsPostAction implements PostAction {

    protected final AbstractStatisticsTransparentTestBase test;

    BaseStatisticsPostAction(AbstractStatisticsTransparentTestBase test) {
      this.test = test;
    }

    protected void waitForAllNodesToConnectToGateway(final int nodeCount) throws IOException, InterruptedException {
      final JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", test.getAdminPort());
      final MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

      final StatisticsGatewayMBean stat_gateway = (StatisticsGatewayMBean) MBeanServerInvocationHandler
          .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

      waitForAllNodesToConnectToGateway(stat_gateway, nodeCount);
    }

    protected void waitForAllNodesToConnectToGateway(final StatisticsGatewayMBean statGateway, final int nodeCount)
        throws InterruptedException {
      int currentNodeCount;
      while ((currentNodeCount = statGateway.getConnectedAgentChannelIDs().length) < nodeCount) {
        Thread.sleep(500);
        System.out.println("Currently " + currentNodeCount + " nodes connected to gateway, waiting for " + nodeCount);
      }
    }

    public void execute() throws Exception {
      //
    }
  }
}