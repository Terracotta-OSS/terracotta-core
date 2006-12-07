/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionInvalidatorTest extends AbstractAppServerTestCase {

  public SessionInvalidatorTest() {
    // disableAllUntil("2007-01-01");
  }

  public static final class ListenerReportingServlet extends HttpServlet {
    private static Map callCounts = new HashMap();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      final String action = req.getParameter("action");
      final String key = req.getParameter("key");
      String reply = "OK";
      if ("get".equals(action)) {
        reply = key + "=" + req.getSession().getAttribute(key);
      } else if ("set".equals(action)) {
        req.getSession().setAttribute(key, new BindingListener(key));
      } else if ("setwithexception".equals(action)) {
        try {
          req.getSession().setAttribute(key, new BindingListenerWithException(key));
          reply = "Did not get expected exception!";
        } catch (Throwable e) {
          // this is expected
        }
      } else if ("remove".equals(action)) {
        req.getSession().removeAttribute(key);
      } else if ("call_count".equals(action)) {
        reply = key + "=" + getCallCount(key);
      } else if ("setmax".equals(action)) {
        req.getSession().setMaxInactiveInterval(Integer.parseInt(key));
      } else if ("isNew".equals(action)) {
        if (!req.getSession().isNew()) reply = "OLD SESSION!";
      } else if ("isOld".equals(action)) {
        if (req.getSession().isNew()) reply = "NEW SESSION!";
      } else if ("sleep".equals(action)) {
        // lock session and go to sleep
        req.getSession();
        sleep(1000 * Integer.parseInt(key));
        req.getSession().setMaxInactiveInterval(30 * 60);
      } else {
        reply = "INVALID REQUEST";
      }
      resp.getWriter().print(reply);
      resp.flushBuffer();
    }

    private void sleep(int i) {
      try {
        Date now = new Date();
        System.err.println("SERVLET: " + now + ": going to sleep for " + i + " millis");
        Thread.sleep(i);
        now = new Date();
        System.err.println("SERVLET: " + now + ": woke up from sleeping for " + i + " millis");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
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
      System.err.println("### SessionListener.sessionDestroyed() is here!!!");
      ListenerReportingServlet.incrementCallCount("SessionListener.sessionDestroyed");
    }
  }

  public static final class BindingListener implements HttpSessionBindingListener {

    private final String key;

    public BindingListener(String key) {
      System.err.println("### BindingListener is here!!! key = " + key);
      this.key = key;
    }

    public void valueBound(HttpSessionBindingEvent e) {
      // the value being bound must not be in session yet...
      Object o = e.getSession().getAttribute(e.getName());
      if (o == null) ListenerReportingServlet.incrementCallCount("BindingListener.valueBound");
      else System.err.println("### Event sequence violated!!!");
    }

    public void valueUnbound(HttpSessionBindingEvent e) {
      ListenerReportingServlet.incrementCallCount("BindingListener.valueUnbound");
    }

    public String toString() {
      return key;
    }
  }

  public static final class BindingListenerWithException implements HttpSessionBindingListener {
    private final String key;

    public BindingListenerWithException(String key) {
      this.key = key;
    }

    public void valueBound(HttpSessionBindingEvent arg0) {
      ListenerReportingServlet.incrementCallCount("BindingListener.valueBound");
      throw new RuntimeException("Testing Exception Delivery");
    }

    public void valueUnbound(HttpSessionBindingEvent arg0) {
      ListenerReportingServlet.incrementCallCount("BindingListener.valueUnbound");
      throw new RuntimeException("Testing Exception Delivery");
    }

    public String toString() {
      return key;
    }
  }

  private int port;

  public void testInvalidator() throws Exception {
    startDsoServer();

    final int invalidatorSleepSeconds = 1;
    final int defaultMaxIdleSeconds = 5;
    final int waitFactor = 4;
    final Properties props = new Properties();
    props.setProperty("com.terracotta.session.invalidator.sleep", String.valueOf(invalidatorSleepSeconds));
    props.setProperty("com.terracotta.session.maxidle.seconds", String.valueOf(defaultMaxIdleSeconds));
    port = startAppServer(true, props).serverPort();

    HttpClient client = HttpUtil.createHttpClient();

    // first, sanity check
    URL url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class) + "?action=0");
    checkResponse("INVALID REQUEST", url, client);

    // now, put a string into session...
    url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class) + "?action=set&key=attr1");
    checkResponse("OK", url, client);
    // ... and check if it made it there.
    url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class) + "?action=get&key=attr1");
    checkResponse("attr1=attr1", url, client);

    checkCallCount("SessionListener.sessionCreated", 1, client);
    checkCallCount("BindingListener.valueBound", 1, client);

    // now set exception-throwing BindingListener..
    url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class)
                  + "?action=setwithexception&key=attr2");
    checkResponse("OK", url, client);
    // ... and check if it DID NOT made it there.
    url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class) + "?action=get&key=attr2");
    checkResponse("attr2=null", url, client);

    checkCallCount("BindingListener.valueBound", 2, client);
    checkCallCount("BindingListener.valueUnbound", 0, client);

    // set session max idle time
    url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class) + "?action=setmax&key=3");
    checkResponse("OK", url, client);

    Thread.sleep(waitFactor * defaultMaxIdleSeconds * 1000);

    checkCallCount("BindingListener.valueUnbound", 1, client);
    checkCallCount("SessionListener.sessionDestroyed", 1, client);
    // =========================================================
    // by this point we varified that our old session was invalidated successfully while it WAS NOT being used.
    // now let's see what happens if it's in use by a LOOONG-running request
    // =========================================================
    // make sure we got a new, good session
    url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class) + "?action=isNew");
    checkResponse("OK", url, client);

    // set session max idle time
    url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class) + "?action=setmax&key=5");
    checkResponse("OK", url, client);

    // now, get a long-running request a-running
    url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class) + "?action=sleep&key=15");
    checkResponse("OK", url, client);

    // make sure we still got the old session
    url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class) + "?action=isOld");
    checkResponse("OK", url, client);
    checkCallCount("BindingListener.valueUnbound", 1, client);
    checkCallCount("SessionListener.sessionDestroyed", 1, client);

    // now let this session expire
    // give invalidator at least 2 runs...
    // set session max idle time
    url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class) + "?action=setmax&key=5");
    checkResponse("OK", url, client);
    Thread.sleep(waitFactor * defaultMaxIdleSeconds * 1000);
    checkCallCount("BindingListener.valueUnbound", 1, client);
    checkCallCount("SessionListener.sessionDestroyed", 2, client);
  }

  private void checkResponse(String expectedResponse, URL url, HttpClient client) throws ConnectException, IOException {
    System.err.println("=== Send Request [" + (new Date()) + "]: url=[" + url + "]");
    final String actualResponse = HttpUtil.getResponseBody(url, client);
    System.err.println("=== Got Response [" + (new Date()) + "]: url=[" + url + "], response=[" + actualResponse + "]");
    assertTimeDirection();
    assertEquals(expectedResponse, actualResponse);
  }

  private void checkCallCount(final String key, int expectedCount, HttpClient client) throws ConnectException,
      IOException {
    URL url = new URL(createUrl(port, SessionInvalidatorTest.ListenerReportingServlet.class)
                      + "?action=call_count&key=" + key);
    checkResponse(key + "=" + expectedCount, url, client);
  }
}
