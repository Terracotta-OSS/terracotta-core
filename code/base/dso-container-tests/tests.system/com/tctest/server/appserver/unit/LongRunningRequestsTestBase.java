/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.webapp.servlets.LongRunningRequestsServlet;

public abstract class LongRunningRequestsTestBase extends AbstractOneServerDeploymentTest {

  protected static final String CONTEXT = "simple";

  public void testSessionLocking(WebConversation conversation, Thread longRunningRequestThread,
                                 Thread[] shortRequestThreads) throws Exception {
    // make a request to create the session first, so that the JSESSIONID cookie is set for the WebConversation
    WebResponse response1 = request(server0, "cmd=" + LongRunningRequestsServlet.CREATE_SESSION, conversation);
    String serverResponse = response1.getText().trim();
    debug("Got response after creating: " + serverResponse);

    longRunningRequestThread.setDaemon(true);
    for (Thread shortRequestThread : shortRequestThreads) {
      shortRequestThread.setDaemon(true);
    }

    longRunningRequestThread.start();
    // wait for some time before making the other request to make sure the first request has hit the server
    ThreadUtil.reallySleep(1000);
    for (Thread shortRequestThread : shortRequestThreads) {
      shortRequestThread.start();
    }
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    debug("Requesting with JSESSIONID: " + con.getCookieValue("JSESSIONID") + " params=" + params);
    return server.ping("/" + CONTEXT + "/" + CONTEXT + "?" + params, con);
  }

  protected void debug(String string) {
    System.out.println(Thread.currentThread().getName() + ": " + string);
  }

  /**
   * ***** test setup *********
   */
  public static class LongRunningRequestsTestSetupBase extends OneServerTestSetup {

    protected LongRunningRequestsTestSetupBase(Class testClass, String context) {
      super(testClass, context);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("LongRunningRequestsServlet", "/" + CONTEXT + "/*", LongRunningRequestsServlet.class, null,
                         false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      if (isSessionLockingTrue()) tcConfigBuilder.addWebApplication(CONTEXT);
      else tcConfigBuilder.addWebApplicationWithoutSessionLocking(CONTEXT);
    }
  }

}
