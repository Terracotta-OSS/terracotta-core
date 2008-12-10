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
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.webapp.servlets.SessionLockingDeadlockServlet;

public abstract class DeadlockTestBase extends AbstractOneServerDeploymentTest {

  protected static final String CONTEXT = "simple";

  public void testSessionLocking(WebConversation conversation, Thread thread1, Thread thread2) throws Exception {
    // make a request to create the session first, so that the JSESSIONID cookie is set for the WebConversation
    WebResponse response1 = request(server0, "cmd=" + SessionLockingDeadlockServlet.CREATE_SESSION, conversation);
    String serverResponse = response1.getText().trim();
    debug("Got response after creating: " + serverResponse);

    thread1.setDaemon(true);
    thread2.setDaemon(true);

    thread1.start();
    // wait for some time before making the other request to make sure the first request has hit the server
    ThreadUtil.reallySleep(1000);
    thread2.start();
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    DeadlockTestBase.debug("Requesting with JSESSIONID: " + con.getCookieValue("JSESSIONID") + " params=" + params);
    return server.ping("/" + DeadlockTestBase.CONTEXT + "/" + DeadlockTestBase.CONTEXT + "?" + params, con);
  }

  protected static void debug(String string) {
    System.out.println(Thread.currentThread().getName() + ": " + string);
  }

  /**
   * ***** test setup *********
   */
  public static class DeadlockTestSetupBase extends OneServerTestSetup {

    protected DeadlockTestSetupBase(Class testClass, String context) {
      super(testClass, context);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("SessionLockingDeadlockServlet", "/" + CONTEXT + "/*", SessionLockingDeadlockServlet.class,
                         null, false);
    }
  }

}
