/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.WebClient;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Vm;
import com.tctest.webapp.listeners.BindingListenerWithException;
import com.tctest.webapp.listeners.InvalidatorAttributeListener;
import com.tctest.webapp.listeners.InvalidatorBindingListener;
import com.tctest.webapp.listeners.InvalidatorSessionListener;
import com.tctest.webapp.servlets.InvalidatorServlet;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class SessionInvalidatorTest extends AbstractAppServerTestCase {

  private int port;

  public SessionInvalidatorTest() {
    if (Vm.isIBM()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
      return;
    }
    registerListener(InvalidatorAttributeListener.class);
    registerListener(InvalidatorSessionListener.class);
    registerListener(InvalidatorBindingListener.class);
    registerListener(BindingListenerWithException.class);
    registerServlet(InvalidatorServlet.class);
  }

  public void testInvalidator() throws Exception {
    startDsoServer();

    final int invalidatorSleepSeconds = 1;
    final int defaultMaxIdleSeconds = 5;
    final int waitFactor = 4;
    final List props = new ArrayList();
    props.add("-Dcom.tc.session.invalidator.sleep=" + String.valueOf(invalidatorSleepSeconds));
    props.add("-Dcom.tc.session.maxidle.seconds=" + String.valueOf(defaultMaxIdleSeconds));
    props.add("-Dcom.tc.session.debug.invalidate=true");
    port = startAppServer(true, new Properties(), (String[]) props.toArray(new String[] {})).serverPort();

    WebClient client = new WebClient(); // HttpUtil.createWebClient();

    // first, sanity check
    URL url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=0");
    checkResponse("INVALID REQUEST", url, client);

    // now, put a string into session...
    url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=set&key=attr1");
    checkResponse("OK", url, client);
    // ... and check if it made it there.
    url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=get&key=attr1");
    checkResponse("attr1=attr1", url, client);

    checkCallCount("SessionListener.sessionCreated", 1, client);
    checkCallCount("BindingListener.valueBound", 1, client);

    // now set exception-throwing BindingListener..
    url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=setwithexception&key=attr2");
    checkResponse("OK", url, client);
    // ... and check if it DID NOT made it there.
    url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=get&key=attr2");
    checkResponse("attr2=null", url, client);

    checkCallCount("BindingListener.valueBound", 2, client);
    checkCallCount("BindingListener.valueUnbound", 0, client);

    // set session max idle time
    url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=setmax&key=3");
    checkResponse("OK", url, client);

    ThreadUtil.reallySleep(waitFactor * defaultMaxIdleSeconds * 1000);

    checkCallCount("SessionListener.sessionDestroyed", 1, client);
    checkCallCount("BindingListener.valueUnbound", 1, client);

    // =========================================================
    // by this point we varified that our old session was invalidated successfully while it WAS NOT being used.
    // now let's see what happens if it's in use by a LOOONG-running request
    // =========================================================
    // make sure we got a new, good session
    url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=isNew");
    checkResponse("OK", url, client);

    // set session max idle time
    url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=setmax&key=5");
    checkResponse("OK", url, client);

    // now, get a long-running request a-running
    url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=sleep&key=15");
    checkResponse("OK", url, client);

    // make sure we still got the old session
    url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=isOld");
    checkResponse("OK", url, client);
    checkCallCount("BindingListener.valueUnbound", 1, client);
    checkCallCount("SessionListener.sessionDestroyed", 1, client);

    // now let this session expire
    // give invalidator at least 2 runs...
    // set session max idle time
    url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=setmax&key=5");
    checkResponse("OK", url, client);
    Thread.sleep(waitFactor * defaultMaxIdleSeconds * 1000);
    checkCallCount("BindingListener.valueUnbound", 1, client);
    checkCallCount("SessionListener.sessionDestroyed", 2, client);
  }

  private void checkResponse(String expectedResponse, URL url, WebClient client) throws ConnectException, IOException {
    System.err.println("=== Send Request [" + (new Date()) + "]: url=[" + url + "]");
    final String actualResponse = client.getResponseAsString(url);// HttpUtil.getResponseBody(url, client);
    System.err.println("=== Got Response [" + (new Date()) + "]: url=[" + url + "], response=[" + actualResponse + "]");
    assertTimeDirection();
    assertEquals(expectedResponse, actualResponse);
  }

  private void checkCallCount(final String key, int expectedCount, WebClient client) throws ConnectException,
      IOException {
    URL url = new URL(createUrl(port, InvalidatorServlet.class) + "?action=call_count&key=" + key);
    checkResponse(key + "=" + expectedCount, url, client);
  }
}
