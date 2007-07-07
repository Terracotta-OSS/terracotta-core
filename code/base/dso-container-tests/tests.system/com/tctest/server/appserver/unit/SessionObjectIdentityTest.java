/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tctest.webapp.servlets.SessionObjectIdentityTestServlet;

import java.net.URL;


public class SessionObjectIdentityTest extends AbstractAppServerTestCase {

  public SessionObjectIdentityTest() {
    registerServlet(SessionObjectIdentityTestServlet.class);
  }

  public final void testSessions() throws Exception {
    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port = startAppServer(true).serverPort();

    URL url = createUrl(port, SessionObjectIdentityTestServlet.class);

    assertEquals("OK", HttpUtil.getResponseBody(url, client));
    assertEquals("OK", HttpUtil.getResponseBody(url, client));
  }

}