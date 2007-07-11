/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tctest.webapp.listeners.AttributeListener;
import com.tctest.webapp.listeners.BindingListener;
import com.tctest.webapp.listeners.SessionListener;
import com.tctest.webapp.servlets.ListenerReportingServlet;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;

public class SessionEventsTest extends AbstractAppServerTestCase {

  private int port;

  public SessionEventsTest() {
    registerListener(AttributeListener.class);
    registerListener(SessionListener.class);
    registerListener(BindingListener.class);
    registerServlet(ListenerReportingServlet.class);
  }

  public void testListener() throws Exception {
    startDsoServer();

    port = startAppServer(true).serverPort();

    HttpClient client = HttpUtil.createHttpClient();

    // first, sanity check
    URL url = new URL(createUrl(port, ListenerReportingServlet.class) + "?action=0");
    assertEquals("INVALID REQUEST", HttpUtil.getResponseBody(url, client));

    // now, put a string into session...
    url = new URL(createUrl(port, ListenerReportingServlet.class) + "?action=set&key=attr1");
    assertEquals("OK", HttpUtil.getResponseBody(url, client));
    // ... and check if it made it there.
    url = new URL(createUrl(port, ListenerReportingServlet.class) + "?action=get&key=attr1");
    assertEquals("attr1=attr1", HttpUtil.getResponseBody(url, client));

    // check if sessionCreated event got fired
    checkCallCount("SessionListener.sessionCreated", 1, client);

    // check if attributeAdded event got fired
    checkCallCount("AttributeListener.attributeAdded", 1, client);
    checkCallCount("BindingListener.valueBound", 1, client);

    // now, replace the same attribute...
    url = new URL(createUrl(port, ListenerReportingServlet.class) + "?action=set&key=attr1");
    assertEquals("OK", HttpUtil.getResponseBody(url, client));
    checkCallCount("AttributeListener.attributeReplaced", 1, client);
    checkCallCount("BindingListener.valueUnbound", 1, client);
    checkCallCount("BindingListener.valueBound", 2, client);

    // now, remove the attribute
    url = new URL(createUrl(port, ListenerReportingServlet.class) + "?action=remove&key=attr1");
    assertEquals("OK", HttpUtil.getResponseBody(url, client));

    checkCallCount("AttributeListener.attributeRemoved", 1, client);
    checkCallCount("BindingListener.valueUnbound", 2, client);

    // now add an attribute...
    url = new URL(createUrl(port, ListenerReportingServlet.class) + "?action=set&key=attr1");
    assertEquals("OK", HttpUtil.getResponseBody(url, client));
    // ... and check if it made it there
    url = new URL(createUrl(port, ListenerReportingServlet.class) + "?action=get&key=attr1");
    assertEquals("attr1=attr1", HttpUtil.getResponseBody(url, client));

    // ...check if right events got fired
    checkCallCount("SessionListener.sessionCreated", 1, client);
    checkCallCount("AttributeListener.attributeAdded", 2, client);
    checkCallCount("BindingListener.valueBound", 3, client);

    // ...now proactively invalidate the session
    url = new URL(createUrl(port, ListenerReportingServlet.class) + "?action=invalidate");
    assertEquals("OK", HttpUtil.getResponseBody(url, client));

    // ...and check if the next request creates a new session
    url = new URL(createUrl(port, ListenerReportingServlet.class) + "?action=isNew");
    assertEquals("OK", HttpUtil.getResponseBody(url, client));

    // ...now check events counts again
    // ...check if right events got fired
    checkCallCount("SessionListener.sessionCreated", 2, client);
    checkCallCount("SessionListener.sessionDestroyed", 1, client);
    checkCallCount("AttributeListener.attributeAdded", 2, client);
    checkCallCount("BindingListener.valueBound", 3, client);
    checkCallCount("AttributeListener.attributeRemoved", 2, client);
    checkCallCount("BindingListener.valueUnbound", 3, client);
  }

  private void checkCallCount(final String key, int expectedCount, HttpClient client) throws ConnectException,
      IOException {
    URL url = new URL(createUrl(port, ListenerReportingServlet.class) + "?action=call_count&key=" + key);
    assertEquals(key + "=" + expectedCount, HttpUtil.getResponseBody(url, client));
  }
}