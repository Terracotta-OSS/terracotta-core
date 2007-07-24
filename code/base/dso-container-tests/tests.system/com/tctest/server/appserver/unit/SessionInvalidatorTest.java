/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import com.tctest.webapp.listeners.BindingListenerWithException;
import com.tctest.webapp.listeners.InvalidatorAttributeListener;
import com.tctest.webapp.listeners.InvalidatorBindingListener;
import com.tctest.webapp.listeners.InvalidatorSessionListener;
import com.tctest.webapp.servlets.InvalidatorServlet;

import java.util.Date;

import junit.framework.Test;

public class SessionInvalidatorTest extends AbstractOneServerDeploymentTest {
  private static final String CONTEXT                 = "SessionInvalidatorTest";
  private static final String SERVLET                 = "InvalidatorServlet";

  private static final int    invalidatorSleepSeconds = 1;
  private static final int    defaultMaxIdleSeconds   = 5;
  private static final int    waitFactor              = 4;

  public static Test suite() {
    return new SessionInvalidatorTestSetup();
  }

  public void testInvalidator() throws Exception {
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
    checkResponse("attr2=null", "action=get&key=attr2", wc);

    checkCallCount("BindingListener.valueBound", 2, wc);
    checkCallCount("BindingListener.valueUnbound", 0, wc);

    // set session max idle time
    checkResponse("OK", "action=setmax&key=3", wc);

    ThreadUtil.reallySleep(waitFactor * defaultMaxIdleSeconds * 1000);

    checkCallCount("SessionListener.sessionDestroyed", 1, wc);
    checkCallCount("BindingListener.valueUnbound", 1, wc);

    // =========================================================
    // by this point we varified that our old session was invalidated successfully while it WAS NOT being used.
    // now let's see what happens if it's in use by a LOOONG-running request
    // =========================================================
    // make sure we got a new, good session
    checkResponse("OK", "action=isNew", wc);

    // set session max idle time
    checkResponse("OK", "action=setmax&key=5", wc);

    // now, get a long-running request a-running
    checkResponse("OK", "action=sleep&key=15", wc);

    // make sure we still got the old session
    checkResponse("OK", "action=isOld", wc);
    checkCallCount("BindingListener.valueUnbound", 1, wc);
    checkCallCount("SessionListener.sessionDestroyed", 1, wc);

    // now let this session expire
    // give invalidator at least 2 runs...
    // set session max idle time
    checkResponse("OK", "action=setmax&key=5", wc);
    Thread.sleep(waitFactor * defaultMaxIdleSeconds * 1000);
    checkCallCount("BindingListener.valueUnbound", 1, wc);
    checkCallCount("SessionListener.sessionDestroyed", 2, wc);
  }

  private void checkResponse(String expected, String params, WebConversation wc) throws Exception {
    System.err.println("=== Send Request [" + (new Date()) + "]: params=[" + params + "]");
    String actual = request(server1, params, wc);
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

  private static class SessionInvalidatorTestSetup extends OneServerTestSetup {
    public SessionInvalidatorTestSetup() {
      super(SessionInvalidatorTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(SERVLET, "/" + SERVLET + "/*", InvalidatorServlet.class, null, false);
      builder.addListener(InvalidatorAttributeListener.class);
      builder.addListener(InvalidatorSessionListener.class);
      builder.addListener(InvalidatorBindingListener.class);
      builder.addListener(BindingListenerWithException.class);
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT);
      clientConfig.addInstrumentedClass(InvalidatorAttributeListener.class.getName());
      clientConfig.addInstrumentedClass(InvalidatorSessionListener.class.getName());
      clientConfig.addInstrumentedClass(InvalidatorBindingListener.class.getName());
      clientConfig.addInstrumentedClass(BindingListenerWithException.class.getName());
    }

    protected void configureServerParamers(StandardAppServerParameters params) {
      params.appendSysProp("com.tc.session.invalidator.sleep", invalidatorSleepSeconds);
      params.appendSysProp("com.tc.session.maxidle.seconds", defaultMaxIdleSeconds);
      params.appendSysProp("com.tc.session.debug.invalidate", true);
    }
  }
}
