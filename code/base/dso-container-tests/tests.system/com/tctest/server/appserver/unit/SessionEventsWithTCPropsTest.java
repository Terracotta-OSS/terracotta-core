/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.listeners.AttributeListener;
import com.tctest.webapp.listeners.BindingListener;
import com.tctest.webapp.listeners.SessionListener;
import com.tctest.webapp.servlets.ListenerReportingServlet;

import junit.framework.Test;

public class SessionEventsWithTCPropsTest extends AbstractOneServerDeploymentTest {
  private static final String CONTEXT = "SessionEventsWithTCPropsTest";
  private static final String MAPPING = "ListenerReportingServlet";

  public SessionEventsWithTCPropsTest() {
    // DEV-1117
    if (appServerInfo().getId() == AppServerInfo.WEBLOGIC) {
      disableAllUntil("2008-12-30");
    }
  }

  public static Test suite() {
    return new SessionEventsWithTCPropsTestSetup();
  }

  public void testListener() throws Exception {
    if (appServerInfo().getId() == AppServerInfo.JETTY) return;

    WebConversation wc = new WebConversation();

    // first, sanity check
    assertEquals("INVALID REQUEST", request("action=0", wc));

    // now, put a string into session...
    assertEquals("OK", request("action=set&key=attr1", wc));
    // ... and check if it made it there.
    assertEquals("attr1=attr1", request("action=get&key=attr1", wc));

    // check if sessionCreated event got fired
    checkCallCount("SessionListener.sessionCreated", 1, wc);

    // check if attributeAdded event got fired
    checkCallCount("AttributeListener.attributeAdded", 1, wc);
    checkCallCount("BindingListener.valueBound", 1, wc);

    // now, replace the same attribute...
    assertEquals("OK", request("action=set&key=attr1", wc));
    checkCallCount("AttributeListener.attributeReplaced", 1, wc);
    checkCallCount("BindingListener.valueUnbound", 1, wc);
    checkCallCount("BindingListener.valueBound", 2, wc);

    // now, remove the attribute
    assertEquals("OK", request("action=remove&key=attr1", wc));

    checkCallCount("AttributeListener.attributeRemoved", 1, wc);
    checkCallCount("BindingListener.valueUnbound", 2, wc);

    // now add an attribute...
    assertEquals("OK", request("action=set&key=attr1", wc));
    // ... and check if it made it there
    assertEquals("attr1=attr1", request("action=get&key=attr1", wc));

    // ...check if right events got fired
    checkCallCount("SessionListener.sessionCreated", 1, wc);
    checkCallCount("AttributeListener.attributeAdded", 2, wc);
    checkCallCount("BindingListener.valueBound", 3, wc);

    // ...now proactively invalidate the session
    assertEquals("OK", request("action=invalidate", wc));

    // ...and check if the next request creates a new session
    assertEquals("OK", request("action=isNew", wc));

    // ...now check events counts again
    // ...check if right events got fired
    checkCallCount("SessionListener.sessionCreated", 2, wc);
    checkCallCount("SessionListener.sessionDestroyed", 1, wc);
    checkCallCount("AttributeListener.attributeAdded", 2, wc);
    checkCallCount("BindingListener.valueBound", 3, wc);
    checkCallCount("AttributeListener.attributeRemoved", 2, wc);
    checkCallCount("BindingListener.valueUnbound", 3, wc);
  }

  private void checkCallCount(final String key, int expectedCount, WebConversation wc) throws Exception {
    String response = request("action=call_count&key=" + key, wc);
    assertEquals(key + "=" + expectedCount, response);
  }

  private String request(String params, WebConversation wc) throws Exception {
    return server0.ping("/" + CONTEXT + "/" + MAPPING + "?" + params, wc).getText().trim();
  }

  private static class SessionEventsWithTCPropsTestSetup extends OneServerTestSetup {

    public SessionEventsWithTCPropsTestSetup() {
      super(SessionEventsWithTCPropsTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("ListenerReportingServlet", "/" + MAPPING + "/*", ListenerReportingServlet.class, null, false);
      builder.addListener(AttributeListener.class);
      builder.addListener(SessionListener.class);
      builder.addListener(BindingListener.class);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplication(CONTEXT);
      /**
       * these 2 listeners are intentionally commented out and will be set through tc.properties instead making sure
       * they could be picked up by app servers tcConfigBuilder.addInstrumentedClass(AttributeListener.class.getName());
       * tcConfigBuilder.addInstrumentedClass(SessionListener.class.getName());
       */
      tcConfigBuilder.addInstrumentedClass(BindingListener.class.getName());
    }

    protected void configureServerParamers(StandardAppServerParameters params) {
      params.appendSysProp("com.tc.session.attribute.listeners", AttributeListener.class.getName());
      params.appendSysProp("com.tc.session.listeners", SessionListener.class.getName());
    }
  }
}