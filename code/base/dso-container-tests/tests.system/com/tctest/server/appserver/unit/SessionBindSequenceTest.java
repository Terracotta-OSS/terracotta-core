/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.webapp.listeners.BindingListener;
import com.tctest.webapp.listeners.BindingListenerWithException;
import com.tctest.webapp.listeners.BindingSequenceListener;
import com.tctest.webapp.listeners.SessionListener;
import com.tctest.webapp.servlets.SessionBindSequenceTestServlet;

import java.util.Date;

import junit.framework.Test;

public class SessionBindSequenceTest extends AbstractOneServerDeploymentTest {
  private static final String CONTEXT                 = "SessionBindSequenceTest";
  private static final String SERVLET                 = "SessionBindSequenceTestServlet";

  private static final int    invalidatorSleepSeconds = 1;
  private static final int    defaultMaxIdleSeconds   = 5;
  private static final int    waitFactor              = 4;

  public static Test suite() {
    return new SessionBindSequenceTestSetup();
  }

  public void testBindSequence() throws Exception {
    WebConversation wc = new WebConversation();
    // first, sanity check
    checkResponse("INVALID REQUEST", "action=0", wc);

    // now, put a string into session...
    checkResponse("OK", "action=set&key=attr1", wc);
    // ... and check if it made it there.
    checkResponse("attr1=attr1", "action=get&key=attr1", wc);

    checkCallCount("SessionListener.sessionCreated", 1, wc);
    checkCallCount("BindingListener.valueBound", 1, wc);

    // now set exception-throwing BindingListener..
    checkResponse("OK", "action=setwithexception&key=attr2", wc);
    // ... and check if it DID NOT made it there.
    String expectedVal = "null";
    checkResponse("attr2=" + expectedVal, "action=get&key=attr2", wc);

    checkCallCount("BindingListener.valueBound", 2, wc);
    checkCallCount("BindingListener.valueUnbound", 0, wc);

    // now set sequence-checking BindingListener..
    checkResponse("OK", "action=setwithsequence&key=attr3", wc);
    // ... and check if it made it there.
    checkResponse("attr3=attr3", "action=get&key=attr3", wc);

    int bslBoundCount = 1;
    // Note that attr3 will still be Unbound
    int bslUnboundCount = 1;
    checkCallCount("BindSequenceListener.valueBound", bslBoundCount, wc);

    // force session invalidation
    checkResponse("OK", "action=setmax&key=3", wc);

    ThreadUtil.reallySleep(waitFactor * defaultMaxIdleSeconds * 1000);

    checkCallCount("SessionListener.sessionDestroyed", 1, wc);
    checkCallCount("BindingListener.valueUnbound", 1, wc);
    checkCallCount("BindSequenceListener.valueUnbound", bslUnboundCount, wc);
  }

  private void checkResponse(String expected, String params, WebConversation wc) throws Exception {
    System.err.println("=== Send Request [" + (new Date()) + "]: params=[" + params + "]");
    String actual = request(server0, params, wc);
    System.err.println("=== Got Response [" + (new Date()) + "]: params=[" + params + "], response=[" + actual + "]");
    assertTimeDirection();
    assertEquals(expected, actual);
  }

  private void checkCallCount(final String key, int expectedCount, WebConversation wc) throws Exception {
    checkResponse(key + "=" + expectedCount, "action=call_count&key=" + key, wc);
  }

  private String request(WebApplicationServer server, String params, WebConversation wc) throws Exception {
    return server.ping("/" + CONTEXT + "/" + SERVLET + "?" + params, wc).getText().trim();
  }

  private static class SessionBindSequenceTestSetup extends OneServerTestSetup {
    public SessionBindSequenceTestSetup() {
      super(SessionBindSequenceTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(SERVLET, "/" + SERVLET + "/*", SessionBindSequenceTestServlet.class, null, false);
      builder.addListener(SessionListener.class);
      builder.addListener(BindingListener.class);
      builder.addListener(BindingSequenceListener.class);
      builder.addListener(BindingListenerWithException.class);
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT);
      clientConfig.addInstrumentedClass(SessionListener.class.getName());
      clientConfig.addInstrumentedClass(BindingListener.class.getName());
      clientConfig.addInstrumentedClass(BindingSequenceListener.class.getName());
      clientConfig.addInstrumentedClass(BindingListenerWithException.class.getName());
    }

    protected void configureServerParamers(StandardAppServerParameters params) {
      params.appendSysProp("com.tc.session.invalidator.sleep", invalidatorSleepSeconds);
      params.appendSysProp("com.tc.session.maxidle.seconds", defaultMaxIdleSeconds);
      params.appendSysProp("com.tc.session.debug.invalidate", true);
    }
  }
}
