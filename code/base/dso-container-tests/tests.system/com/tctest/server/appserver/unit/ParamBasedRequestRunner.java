/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.util.Assert;

public class ParamBasedRequestRunner implements Runnable {
  private String               param;
  private WebApplicationServer server;
  private WebConversation      conversation;

  public ParamBasedRequestRunner(String param, WebApplicationServer server, WebConversation conversation) {
    this.param = param;
    this.server = server;
    this.conversation = conversation;
  }

  public void run() {
    try {
      DeadlockTestBase.debug("Making param-based-request with param: " + param);
      WebResponse response = request(param, conversation);
      String serverResponse = response.getText().trim();
      System.out.println("Server Response (for request with param=" + param + "): " + serverResponse);
      Assert.assertEquals("OK", serverResponse.trim());
    } catch (Exception e) {
      debug("Got Exception in ParamBasedRequestRunner (for request with param=" + param + "): " + e);
      e.printStackTrace();
    }
  }

  protected static void debug(String string) {
    System.out.println(Thread.currentThread().getName() + ": " + string);
  }

  private WebResponse request(String params, WebConversation con) throws Exception {
    DeadlockTestBase.debug("Requesting with JSESSIONID: " + con.getCookieValue("JSESSIONID") + " params=" + params);
    return server.ping("/" + DeadlockTestBase.CONTEXT + "/" + DeadlockTestBase.CONTEXT + "?" + params, con);
  }
}