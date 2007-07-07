/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tctest.webapp.servlets.ServerHopCookieRewriteTestServlet;

import java.net.URL;
import java.util.Properties;

public final class ServerHopCookieRewriteTest extends AbstractAppServerTestCase {

  public ServerHopCookieRewriteTest() {
    registerServlet(ServerHopCookieRewriteTestServlet.class);
  }

  public void testSessions() throws Exception {
    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();
    String[] args = new String[] { "-Dcom.tc.session.delimiter=" + ServerHopCookieRewriteTestServlet.DLM };

    int port0 = startAppServer(true, new Properties(), args).serverPort();
    int port1 = startAppServer(true, new Properties(), args).serverPort();

    URL url0 = new URL(createUrl(port0, ServerHopCookieRewriteTestServlet.class) + "?server=0");
    URL url1 = new URL(createUrl(port1, ServerHopCookieRewriteTestServlet.class) + "?server=1");
    URL url2 = new URL(createUrl(port0, ServerHopCookieRewriteTestServlet.class) + "?server=2");
    assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    assertEquals("OK", HttpUtil.getResponseBody(url1, client));
    assertEquals("OK", HttpUtil.getResponseBody(url2, client));
  }

}
