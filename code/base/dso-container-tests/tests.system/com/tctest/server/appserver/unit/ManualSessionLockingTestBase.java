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
import com.tctest.webapp.servlets.ManualSessionLockingServlet;

public abstract class ManualSessionLockingTestBase extends AbstractOneServerDeploymentTest {

  public static final String CONTEXT = "simple";

  public void testSessionLocking(WebConversation conversation, Thread waitingRequestThread,
                                 Thread notifyingRequestThread) throws Exception {
    // make a request to create the session first, so that the JSESSIONID cookie is set for the WebConversation
    WebResponse response1 = request(server0, "cmd=" + ManualSessionLockingServlet.CREATE_SESSION, conversation);
    String serverResponse = response1.getText().trim();
    debug("Got response after creating: " + serverResponse);

    waitingRequestThread.setDaemon(true);
    notifyingRequestThread.setDaemon(true);

    waitingRequestThread.start();
    // wait for some time before making the notify request to make sure the wait request has hit the server
    ThreadUtil.reallySleep(1000);
    notifyingRequestThread.start();
  }

  private static WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    debug("Requesting with JSESSIONID: " + con.getCookieValue("JSESSIONID") + " params=" + params);
    return server.ping("/" + CONTEXT + "/" + CONTEXT + "?" + params, con);
  }

  protected static void debug(String string) {
    System.out.println(Thread.currentThread().getName() + ": " + string);
  }

  /**
   * ***** test setup *********
   */
  protected static class ManualSessionLockingSetup extends OneServerTestSetup {

    protected ManualSessionLockingSetup(Class testClass, String context) {
      super(testClass, context);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("ManualSessionLockingServlet", "/" + CONTEXT + "/*", ManualSessionLockingServlet.class, null,
                         false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      if (isSessionLockingTrue()) tcConfigBuilder.addWebApplication(CONTEXT);
      else tcConfigBuilder.addWebApplicationWithoutSessionLocking(CONTEXT);
    }
  }

}
