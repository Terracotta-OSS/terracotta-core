/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpNotFoundException;
import com.meterware.httpunit.WebConversation;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.TCServerImpl;
import com.tc.util.Assert;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;

public class StatisticsRestInterfaceDisabledTest extends AbstractStatisticsTransparentTestBase {

  @Override
  protected void setExtraJvmArgs(final ArrayList jvmArgs) {
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.CVT_REST_INTERFACE_ENABLED, "false");
    System.setProperty(TCPropertiesImpl.tcSysProp(TCPropertiesConsts.CVT_REST_INTERFACE_ENABLED), "false");
    jvmArgs.add("-D" + TCPropertiesImpl.tcSysProp(TCPropertiesConsts.CVT_REST_INTERFACE_ENABLED) + "=false");
  }

  @Override
  protected void duringRunningCluster() throws Exception {
    waitForAllNodesToConnectToGateway(StatisticsRestInterfaceTestApp.NODE_COUNT+1);

    final String urlBase = "http://localhost:" + getDsoPort() + TCServerImpl.STATISTICS_GATHERER_SERVLET_PREFIX + "/";

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

  @Override
  protected Class getApplicationClass() {
    return StatisticsRestInterfaceTestApp.class;
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsRestInterfaceTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}