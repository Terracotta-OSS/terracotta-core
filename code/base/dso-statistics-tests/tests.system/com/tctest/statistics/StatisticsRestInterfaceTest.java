/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.tc.server.TCServerImpl;
import com.tc.util.Assert;
import com.tctest.TransparentTestIface;

public class StatisticsRestInterfaceTest extends AbstractStatisticsTransparentTestBase {

  @Override
  protected Class getApplicationClass() {
    return StatisticsRestInterfaceTestApp.class;
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsRestInterfaceTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected void loadPostActions() {
    addPostAction(new StatisticsPostAction(this));
  }

  private static class StatisticsPostAction extends BaseStatisticsPostAction {

    public StatisticsPostAction(AbstractStatisticsTransparentTestBase test) {
      super(test);
    }

    @Override
    public void execute() throws Exception {
      waitForAllNodesToConnectToGateway(StatisticsRestInterfaceTestApp.NODE_COUNT + 1);

      final String urlBase = "http://localhost:" + test.getDsoPort() + TCServerImpl.STATISTICS_GATHERER_SERVLET_PREFIX
                             + "/";

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
}