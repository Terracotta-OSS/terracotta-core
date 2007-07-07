/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tctest.webapp.servlets.SimpleDsoSessionsTestServlet;

import java.net.URL;


/**
 * Simplest test case for DSO. This class should be used as a model for building container based tests. A feature which
 * was omitted in this test is the overloaded startAppServer() method which also takes a properties file. These
 * properties will then be available to the innerclass servlet below as system properties. View the superclass
 * description for more information about general usage.
 */
public class SimpleDsoSessionsTest extends AbstractAppServerTestCase {

  public SimpleDsoSessionsTest() {
    registerServlet(SimpleDsoSessionsTestServlet.class);
  }

  public final void testSessions() throws Exception {

    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port0 = startAppServer(true).serverPort();
    int port1 = startAppServer(true).serverPort();

    URL url0 = new URL(createUrl(port0, SimpleDsoSessionsTestServlet.class) + "?server=0");
    URL url1 = new URL(createUrl(port1, SimpleDsoSessionsTestServlet.class) + "?server=1");

    assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    assertEquals("OK", HttpUtil.getResponseBody(url1, client));
  }

  public final void testSynchronousWriteSessions() throws Exception {

    setSynchronousWrite(true);
    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port0 = startAppServer(true).serverPort();
    int port1 = startAppServer(true).serverPort();

    URL url0 = new URL(createUrl(port0, SimpleDsoSessionsTestServlet.class) + "?server=0");
    URL url1 = new URL(createUrl(port1, SimpleDsoSessionsTestServlet.class) + "?server=1");

    assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    assertEquals("OK", HttpUtil.getResponseBody(url1, client));
  }
}
