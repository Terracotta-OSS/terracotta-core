/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.object.util.ReadOnlyException;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.Assert;
import com.tctest.webapp.servlets.UnlockedSessionObjectServlet;

import java.io.IOException;

public abstract class UnlockedSessionObjectTestBase extends AbstractOneServerDeploymentTest {

  public static final String CONTEXT = "simple";

  public void testSessionLocking() throws Exception {
    WebConversation conversation = new WebConversation();
    // make a request to create the session first, so that the JSESSIONID cookie is set for the WebConversation
    {
      String serverResponse = getResponseForRequest(server0, conversation,
                                                    "cmd=" + UnlockedSessionObjectServlet.CREATE_SESSION);
      Assert.assertEquals("OK", serverResponse);
    }

    {
      String serverResponse = getResponseForRequest(server0, conversation, "cmd=" + UnlockedSessionObjectServlet.INSERT);
      Assert.assertEquals("OK", serverResponse);
    }

    {
      String serverResponse = getResponseForRequest(server0, conversation,
                                                    "cmd=" + UnlockedSessionObjectServlet.MUTATE_WITHOUT_LOCK);
      if (isSessionLockingTrue()) {
        Assert.assertEquals("OK", serverResponse);
      } else {
        Assert.assertEquals(ReadOnlyException.class.getName(), serverResponse);
      }
    }

    {
      String serverResponse = getResponseForRequest(server0, conversation,
                                                    "cmd=" + UnlockedSessionObjectServlet.MUTATE_WITH_LOCK);
      Assert.assertEquals("OK", serverResponse);
    }

  }

  private String getResponseForRequest(WebApplicationServer server, WebConversation conversation, String param)
      throws Exception, IOException {
    WebResponse response = request(server, conversation, param);
    String serverResponse = response.getText().trim();
    debug("Got response (for param=" + param + "): " + serverResponse);
    return serverResponse;
  }

  private static WebResponse request(WebApplicationServer server, WebConversation con, String params) throws Exception {
    debug("Requesting with JSESSIONID: " + con.getCookieValue("JSESSIONID") + " params=" + params);
    return server.ping("/" + CONTEXT + "/" + CONTEXT + "?" + params, con);
  }

  protected static void debug(String string) {
    System.out.println(Thread.currentThread().getName() + ": " + string);
  }

  /**
   * ***** test setup *********
   */
  protected static class UnlockedSessionObjectTestSetup extends OneServerTestSetup {

    protected UnlockedSessionObjectTestSetup(Class testClass, String context) {
      super(testClass, context);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      if (isSessionLockingTrue()) tcConfigBuilder.addWebApplication(CONTEXT);
      else tcConfigBuilder.addWebApplicationWithoutSessionLocking(CONTEXT);
      tcConfigBuilder.addInstrumentedClass(UnlockedSessionObjectServlet.class.getName());
      tcConfigBuilder.addInstrumentedClass(UnlockedSessionObjectServlet.class.getName() + "$DumbData");
      String methodExpression = "* " + UnlockedSessionObjectServlet.class.getName() + ".mutateWithLock(..)";
      tcConfigBuilder.addAutoLock(methodExpression, "write");
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(UnlockedSessionObjectServlet.class.getName(), "/" + CONTEXT + "/*",
                         UnlockedSessionObjectServlet.class, null, false);
    }
  }

}
