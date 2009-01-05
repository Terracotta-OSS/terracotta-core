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
import com.tctest.webapp.listeners.InvalidatorAttributeListener;
import com.tctest.webapp.listeners.InvalidatorBindingListener;
import com.tctest.webapp.listeners.InvalidatorSessionListener;
import com.tctest.webapp.servlets.LongRunningInvalidatorServlet;

import java.util.Date;

import junit.framework.Test;

public class LongRunningSessionInvalidatorTest extends AbstractOneServerDeploymentTest {
  protected static final String CONTEXT                 = "LongRunningSessionInvalidatorTest";
  private static final String   SERVLET                 = "LongRunningInvalidatorServlet";

  private static final int      invalidatorSleepSeconds = 1;
  private static final int      defaultMaxIdleSeconds   = 5;
  private static final int      waitFactor              = 4;

  public static Test suite() {
    return new LongRunningSessionInvalidatorTestSetup();
  }

  public void testInvalidator() throws Exception {
    final WebConversation wc = new WebConversation();
    // first, sanity check
    checkResponse("INVALID REQUEST", "action=0", wc);

    // make sure we got a new, good session
    checkResponse("OK", "action=isNew", wc);

    // now, put a string into session...
    checkResponse("OK", "action=set&key=attr1", wc);
    checkCallCount("BindingListener.valueBound", 1, wc);

    // set session max idle time
    debug("Setting maxInactiveInterval to 5 secs");
    checkResponse("OK", "action=setmax&key=5", wc);

    debug("Making long running request...duration 15 secs");
    // now, get a long-running request running
    checkResponse("OK", "action=sleep&key=25", wc);

    // make sure we still got the old session
    checkResponse("OK", "action=isOld", wc);
    checkCallCount("BindingListener.valueUnbound", 0, wc);
    checkCallCount("SessionListener.sessionDestroyed", 0, wc);

    // now let this session expire
    // give invalidator at least 2 runs...
    // set session max idle time
    checkResponse("OK", "action=setmax&key=5", wc);
    Thread.sleep(waitFactor * defaultMaxIdleSeconds * 1000);
    checkCallCount("BindingListener.valueUnbound", 1, wc);
    checkCallCount("SessionListener.sessionDestroyed", 1, wc);

    // make sure we got a new session
    checkResponse("OK", "action=isNew", wc);
    // add some attribute
    checkResponse("OK", "action=set&key=attr1", wc);
    // explicitly invalidate the session...
    checkResponse("OK", "action=invalidate", wc);
    // ... and create a new one..
    checkResponse("OK", "action=isNew", wc);

    // ... add some attribute ...
    checkResponse("OK", "action=set&key=attr1", wc);
    // ... again explicitly invalidate and access session, put an attribute in the session
    checkResponse("OK", "action=invalidateAndAccess", wc);
  }

  private void debug(String string) {
    System.out.println(":::::::::::::::::: " + string);

  }

  protected void checkResponse(String expected, String params, WebConversation wc) throws Exception {
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

  protected static class LongRunningSessionInvalidatorTestSetup extends OneServerTestSetup {
    public LongRunningSessionInvalidatorTestSetup() {
      super(LongRunningSessionInvalidatorTest.class, CONTEXT);
    }

    public LongRunningSessionInvalidatorTestSetup(Class testClass, String context) {
      super(testClass, context);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(SERVLET, "/" + SERVLET + "/*", LongRunningInvalidatorServlet.class, null, false);
      builder.addListener(InvalidatorAttributeListener.class);
      builder.addListener(InvalidatorSessionListener.class);
      builder.addListener(InvalidatorBindingListener.class);
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      if (isSessionLockingTrue()) clientConfig.addWebApplication(CONTEXT);
      else clientConfig.addWebApplicationWithoutSessionLocking(CONTEXT);
      clientConfig.addInstrumentedClass(InvalidatorAttributeListener.class.getName());
      clientConfig.addInstrumentedClass(InvalidatorSessionListener.class.getName());
      clientConfig.addInstrumentedClass(InvalidatorBindingListener.class.getName());
    }

    protected void configureServerParamers(StandardAppServerParameters params) {
      params.appendSysProp("com.tc.session.invalidator.sleep", invalidatorSleepSeconds);
      params.appendSysProp("com.tc.session.maxidle.seconds", defaultMaxIdleSeconds);
      params.appendSysProp("com.tc.session.debug.invalidate", true);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }
}
