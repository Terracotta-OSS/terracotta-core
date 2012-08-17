/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.statistics;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.tc.server.TCServerImpl;
import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class StatisticsRestInterfaceTest extends AbstractStatisticsTestBase {

  public StatisticsRestInterfaceTest(TestConfig testConfig) {
    super(testConfig);
  }

  @Override
  protected void duringRunningCluster() throws Exception {
    waitForAllNodesToConnectToGateway(StatisticsGatewayTestClient.NODE_COUNT + 1);

    final String urlBase = "http://localhost:" + getGroupData(0).getDsoPort(0)
                           + TCServerImpl.STATISTICS_GATHERER_SERVLET_PREFIX + "/";

    WebConversation wc = new WebConversation();

    WebRequest requestCommands = new GetMethodWebRequest(urlBase);
    WebResponse responseCommands = wc.getResponse(requestCommands);
    Assert.assertEquals(200, responseCommands.getResponseCode());
    Assert.assertTrue(responseCommands.getText().length() > 0);

    WebRequest requestStartup = new GetMethodWebRequest(urlBase + "startup");
    WebResponse responseStartup = wc.getResponse(requestStartup);
    Assert.assertEquals(200, responseStartup.getResponseCode());
    Assert.assertEquals("OK", responseStartup.getText());

    WebRequest requestShutdown = new GetMethodWebRequest(urlBase + "shutdown");
    WebResponse responseShutdown = wc.getResponse(requestShutdown);
    Assert.assertEquals(200, responseShutdown.getResponseCode());
    Assert.assertEquals("OK", responseShutdown.getText());
  }

}
