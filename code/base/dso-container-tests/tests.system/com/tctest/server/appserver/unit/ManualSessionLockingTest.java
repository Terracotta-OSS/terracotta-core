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
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.webapp.servlets.ManualSessionLockingServlet;

import junit.framework.Test;

public class ManualSessionLockingTest extends AbstractOneServerDeploymentTest {

  private static final String CONTEXT = "simple";

  public ManualSessionLockingTest() {
    disableAllUntil("2009-01-01");
  }

  public static Test suite() {
    return new ManualSessionLockingSetup();
  }

  public void testSessionLocking() throws Exception {
    WebConversation conversation = new WebConversation();
    // make a request to create the session first, so that the JSESSIONID cookie is set for the WebConversation
    WebResponse response1 = request(server0, "cmd=" + ManualSessionLockingServlet.CREATE_SESSION, conversation);
    String serverResponse = response1.getText().trim();
    debug("Got response after creating: " + serverResponse);

    Thread waitingRequestThread = new Thread(new WaitingRequest(server0, conversation));
    Thread notifyingRequestThread = new Thread(new NotifyingRequest(server0, conversation));
    waitingRequestThread.setDaemon(true);
    notifyingRequestThread.setDaemon(true);

    waitingRequestThread.start();
    // wait for some time before making the notify request to make sure the wait request has hit the server
    ThreadUtil.reallySleep(1000);
    notifyingRequestThread.start();

    int waitTimeMillis = 30 * 1000;
    ThreadUtil.reallySleep(waitTimeMillis);

    if (waitingRequestThread.isAlive()) {
      waitingRequestThread.interrupt();
      if (notifyingRequestThread.isAlive()) {
        notifyingRequestThread.interrupt();
      }
      Assert.fail("Requests are deadlocked. Waiting Request did not complete");
    }
    debug("Test passed");
  }

  private static WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    debug("Requesting with JSESSIONID: " + con.getCookieValue("JSESSIONID") + " params=" + params);
    return server.ping("/" + CONTEXT + "/" + CONTEXT + "?" + params, con);
  }

  private static void debug(String string) {
    System.out.println(Thread.currentThread().getName() + ": " + string);
  }

  private static class WaitingRequest implements Runnable {

    private WebApplicationServer server;
    private WebConversation      conversation;

    public WaitingRequest(WebApplicationServer server, WebConversation conversation) {
      this.server = server;
      this.conversation = conversation;
    }

    public void run() {
      try {
        debug("Making request that will wait...");
        WebResponse response1 = request(server, "cmd=" + ManualSessionLockingServlet.WAIT, conversation);
        String serverResponse = response1.getText().trim();
        System.out.println("Server Response from wait: " + serverResponse);
        assertEquals("OK", serverResponse.trim());
      } catch (Exception e) {
        debug("Got Exception in WaitingRequest: " + e);
        e.printStackTrace();
      }
    }
  }

  private static class NotifyingRequest implements Runnable {

    private WebApplicationServer server;
    private WebConversation      conversation;

    public NotifyingRequest(WebApplicationServer server, WebConversation conversation) {
      this.server = server;
      this.conversation = conversation;
    }

    public void run() {
      try {
        debug("Making request that will notify...");
        WebResponse response1 = request(server, "cmd=" + ManualSessionLockingServlet.NOTIFY, conversation);
        String serverResponse = response1.getText().trim();
        System.out.println("Server Response from notify: " + serverResponse);
        assertEquals("OK", serverResponse.trim());
      } catch (Exception e) {
        debug("Got Exception in NotifyingRequest: " + e);
        e.printStackTrace();
      }
    }
  }

  /**
   * ***** test setup *********
   */
  private static class ManualSessionLockingSetup extends OneServerTestSetup {

    public ManualSessionLockingSetup() {
      super(ManualSessionLockingTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("ManualSessionLockingServlet", "/" + CONTEXT + "/*", ManualSessionLockingServlet.class, null,
                         false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplicationWithSessionLocking(CONTEXT);
    }
  }

}
