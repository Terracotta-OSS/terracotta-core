/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tc.util.runtime.Vm;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionEventsTest extends AbstractAppServerTestCase {
  
  public SessionEventsTest() {
    if (Vm.isIBM()) {
      disableAllUntil("2007-07-03");
    }
  }

  public static final class ListenerReportingServlet extends HttpServlet {
    private static Map callCounts = new HashMap();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      // requests should have 2 standard params: action=get|set|remove|call_count and key
      final String action = req.getParameter("action");
      final String key = req.getParameter("key");
      String reply = "OK";
      if ("get".equals(action)) {
        reply = key + "=" + req.getSession().getAttribute(key);
      } else if ("set".equals(action)) {
        req.getSession().setAttribute(key, new BindingListener(key));
      } else if ("remove".equals(action)) {
        req.getSession().removeAttribute(key);
      } else if ("call_count".equals(action)) {
        reply = key + "=" + getCallCount(key);
      } else if ("invalidate".equals(action)) {
        req.getSession().invalidate();
      } else if ("isNew".equals(action)) {
        if (!req.getSession().isNew()) reply = "ERROR: OLD SESSION!";
      } else {
        reply = "INVALID REQUEST";
      }
      resp.getWriter().print(reply);
      resp.flushBuffer();
    }

    private synchronized static int getCallCount(String key) {
      Integer i = (Integer) callCounts.get(key);
      return i == null ? 0 : i.intValue();
    }

    public synchronized static void incrementCallCount(String key) {
      Integer i = (Integer) callCounts.get(key);
      if (i == null) {
        i = new Integer(1);
      } else {
        i = new Integer(i.intValue() + 1);
      }
      callCounts.put(key, i);
    }
  }

  public static final class AttributeListener implements HttpSessionAttributeListener {

    public AttributeListener() {
      System.err.println("### AttributeListener() is here!!!");
    }

    public void attributeAdded(HttpSessionBindingEvent httpsessionbindingevent) {
      ListenerReportingServlet.incrementCallCount("AttributeListener.attributeAdded");
      System.err.println("### AttributeListener.attributeAdded() is here!!!");
    }

    public void attributeRemoved(HttpSessionBindingEvent httpsessionbindingevent) {
      System.err.println("### AttributeListener.attributeRemoved() is here!!!");
      ListenerReportingServlet.incrementCallCount("AttributeListener.attributeRemoved");
    }

    public void attributeReplaced(HttpSessionBindingEvent httpsessionbindingevent) {
      System.err.println("### AttributeListener.attributeReplaced() is here!!!");
      ListenerReportingServlet.incrementCallCount("AttributeListener.attributeReplaced");
    }
  }

  public static final class SessionListener implements HttpSessionListener {
    public SessionListener() {
      System.err.println("### SessionListener() is here!!!");
    }

    public void sessionCreated(HttpSessionEvent httpsessionevent) {
      System.err.println("### SessionListener.sessionCreated() is here!!!");
      ListenerReportingServlet.incrementCallCount("SessionListener.sessionCreated");
    }

    public void sessionDestroyed(HttpSessionEvent httpsessionevent) {
      testAttributeAccess(httpsessionevent.getSession());
      System.err.println("### SessionListener.sessionDestroyed() is here!!!");
      ListenerReportingServlet.incrementCallCount("SessionListener.sessionDestroyed");
    }

    private void testAttributeAccess(HttpSession session) {
      // While session destroyed event is being called, you should still be able to get
      // attributes

      String[] attrs = session.getValueNames();
      if (attrs == null || attrs.length == 0) {
        // please make at least one attribute is present
        throw new AssertionError("Attributes should be present during this phase");
      }

      for (int i = 0; i < attrs.length; i++) {
        String attr = attrs[i];
        session.getAttribute(attr);
      }
    }
  }

  public static final class BindingListener implements HttpSessionBindingListener {

    private final String key;

    public BindingListener(String key) {
      System.err.println("### BindingListener is here!!! key = " + key);
      this.key = key;
    }

    public void valueBound(HttpSessionBindingEvent e) {
      System.err.println("### BindingListener.valueBound");
      // the value being bound must not be in session yet...
      ListenerReportingServlet.incrementCallCount("BindingListener.valueBound");
    }

    public void valueUnbound(HttpSessionBindingEvent e) {
      System.err.println("### BindingListener.valueUnbound");
      // the value being unbound must not be in session already...
      ListenerReportingServlet.incrementCallCount("BindingListener.valueUnbound");
    }

    public String toString() {
      return key;
    }
  }

  private int port;

  public void testListener() throws Exception {
    startDsoServer();

    port = startAppServer(true).serverPort();

    HttpClient client = HttpUtil.createHttpClient();

    // first, sanity check
    URL url = new URL(createUrl(port, SessionEventsTest.ListenerReportingServlet.class) + "?action=0");
    assertEquals("INVALID REQUEST", HttpUtil.getResponseBody(url, client));

    // now, put a string into session...
    url = new URL(createUrl(port, SessionEventsTest.ListenerReportingServlet.class) + "?action=set&key=attr1");
    assertEquals("OK", HttpUtil.getResponseBody(url, client));
    // ... and check if it made it there.
    url = new URL(createUrl(port, SessionEventsTest.ListenerReportingServlet.class) + "?action=get&key=attr1");
    assertEquals("attr1=attr1", HttpUtil.getResponseBody(url, client));

    // check if sessionCreated event got fired
    checkCallCount("SessionListener.sessionCreated", 1, client);

    // check if attributeAdded event got fired
    checkCallCount("AttributeListener.attributeAdded", 1, client);
    checkCallCount("BindingListener.valueBound", 1, client);

    // now, replace the same attribute...
    url = new URL(createUrl(port, SessionEventsTest.ListenerReportingServlet.class) + "?action=set&key=attr1");
    assertEquals("OK", HttpUtil.getResponseBody(url, client));
    checkCallCount("AttributeListener.attributeReplaced", 1, client);
    checkCallCount("BindingListener.valueUnbound", 1, client);
    checkCallCount("BindingListener.valueBound", 2, client);

    // now, remove the attribute
    url = new URL(createUrl(port, SessionEventsTest.ListenerReportingServlet.class) + "?action=remove&key=attr1");
    assertEquals("OK", HttpUtil.getResponseBody(url, client));

    checkCallCount("AttributeListener.attributeRemoved", 1, client);
    checkCallCount("BindingListener.valueUnbound", 2, client);

    // now add an attribute...
    url = new URL(createUrl(port, SessionEventsTest.ListenerReportingServlet.class) + "?action=set&key=attr1");
    assertEquals("OK", HttpUtil.getResponseBody(url, client));
    // ... and check if it made it there
    url = new URL(createUrl(port, SessionEventsTest.ListenerReportingServlet.class) + "?action=get&key=attr1");
    assertEquals("attr1=attr1", HttpUtil.getResponseBody(url, client));

    // ...check if right events got fired
    checkCallCount("SessionListener.sessionCreated", 1, client);
    checkCallCount("AttributeListener.attributeAdded", 2, client);
    checkCallCount("BindingListener.valueBound", 3, client);

    // ...now proactively invalidate the session
    url = new URL(createUrl(port, SessionEventsTest.ListenerReportingServlet.class) + "?action=invalidate");
    assertEquals("OK", HttpUtil.getResponseBody(url, client));

    // ...and check if the next request creates a new session
    url = new URL(createUrl(port, SessionEventsTest.ListenerReportingServlet.class) + "?action=isNew");
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
    URL url = new URL(createUrl(port, SessionEventsTest.ListenerReportingServlet.class) + "?action=call_count&key="
                      + key);
    assertEquals(key + "=" + expectedCount, HttpUtil.getResponseBody(url, client));
  }
}
