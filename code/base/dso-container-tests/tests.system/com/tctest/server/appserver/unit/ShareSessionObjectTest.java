/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tctest.webapp.servlets.ShareSessionObjectTestServlet;

import java.net.URL;


public class ShareSessionObjectTest extends AbstractAppServerTestCase {

  public ShareSessionObjectTest() {
    registerServlet(ShareSessionObjectTestServlet.class);
  }

  public final void testSessions() throws Exception {

    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port0 = startAppServer(true).serverPort();
    int port1 = startAppServer(true).serverPort();

    URL url0 = new URL(createUrl(port0, ShareSessionObjectTestServlet.class) + "?cmd=set");
    URL url1 = new URL(createUrl(port1, ShareSessionObjectTestServlet.class) + "?cmd=read");

    assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    assertEquals("OK", HttpUtil.getResponseBody(url1, client));
  }

}
