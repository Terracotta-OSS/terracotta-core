/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.statistics;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpNotFoundException;
import com.meterware.httpunit.WebConversation;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.TCServerImpl;
import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class StatisticsRestInterfaceDisabledTest extends AbstractStatisticsTestBase {

  public StatisticsRestInterfaceDisabledTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getL2Config().addExtraServerJvmArg("-D"
                                                      + TCPropertiesImpl
                                                          .tcSysProp(TCPropertiesConsts.CVT_REST_INTERFACE_ENABLED)
                                                      + "=false");
    testConfig.addTcProperty(TCPropertiesConsts.CVT_REST_INTERFACE_ENABLED, "false");
  }

  @Override
  protected void duringRunningCluster() throws Exception {
    waitForAllNodesToConnectToGateway(StatisticsGatewayTestClient.NODE_COUNT + 1);

    final String urlBase = "http://localhost:" + getGroupData(0).getDsoPort(0)
                           + TCServerImpl.STATISTICS_GATHERER_SERVLET_PREFIX + "/";

    WebConversation wc = new WebConversation();

    try {
      wc.getResponse(new GetMethodWebRequest(urlBase));
      Assert.fail("expected exception");
    } catch (HttpNotFoundException e) {
      // expected
    }

    try {
      wc.getResponse(new GetMethodWebRequest(urlBase + "startup"));
      Assert.fail("expected exception");
    } catch (HttpNotFoundException e) {
      // expected
    }

    try {
      wc.getResponse(new GetMethodWebRequest(urlBase + "shutdown"));
      Assert.fail("expected exception");
    } catch (HttpNotFoundException e) {
      // expected
    }
  }

}
