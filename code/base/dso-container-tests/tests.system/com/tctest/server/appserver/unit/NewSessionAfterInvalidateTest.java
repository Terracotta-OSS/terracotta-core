/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tctest.webapp.servlets.NewSessionAfterInvalidateTestServlet;

import java.net.MalformedURLException;
import java.net.URL;

public class NewSessionAfterInvalidateTest extends AbstractAppServerTestCase {

  public NewSessionAfterInvalidateTest() {
    registerServlet(NewSessionAfterInvalidateTestServlet.class);
  }

  public final void testSessions() throws Exception {
    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port = startAppServer(true).serverPort();

    // no existing session
    assertEquals("OK", HttpUtil.getResponseBody(makeURL(port, 1), client));

    // existing session
    assertEquals("OK", HttpUtil.getResponseBody(makeURL(port, 2), client));
  }

  private URL makeURL(int port, int step) throws MalformedURLException {
    return createUrl(port, NewSessionAfterInvalidateTestServlet.class, "step=" + step);
  }

}
