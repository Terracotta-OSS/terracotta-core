/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tctest.webapp.servlets.RequestForwardTestForwardeeServlet;
import com.tctest.webapp.servlets.RequestForwardTestForwarderServlet;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.Date;


public class RequestForwardTest extends AbstractAppServerTestCase {

  public RequestForwardTest() {
    registerServlet(RequestForwardTestForwarderServlet.class);
    registerServlet(RequestForwardTestForwardeeServlet.class);
  }

  private int port;

  public void testSessionForwardSession() throws Exception {
    startDsoServer();

    port = startAppServer(true).serverPort();

    HttpClient client = HttpUtil.createHttpClient();

    // first, sanity check
    URL url = new URL(createUrl(port, RequestForwardTestForwarderServlet.class) + "?action=0");
    checkResponse("INVALID REQUEST", url, client);

    url = new URL(createUrl(port, RequestForwardTestForwarderServlet.class)
                  + "?action=s-f-s&target=RequestForwardTest-ForwardeeServlet");
    checkResponse("FORWARD GOT SESSION", url, client);
  }

  public void testForwardSession() throws Exception {
    startDsoServer();

    port = startAppServer(true).serverPort();

    HttpClient client = HttpUtil.createHttpClient();

    // first, sanity check
    URL url = new URL(createUrl(port, RequestForwardTestForwarderServlet.class) + "?action=0");
    checkResponse("INVALID REQUEST", url, client);

    url = new URL(createUrl(port, RequestForwardTestForwarderServlet.class) + "?action=n-f-s&target=RequestForwardTest-ForwardeeServlet");
    checkResponse("FORWARD GOT SESSION", url, client);
  }
  
  public void testSessionForward() throws Exception {
    startDsoServer();

    port = startAppServer(true).serverPort();

    HttpClient client = HttpUtil.createHttpClient();

    // first, sanity check
    URL url = new URL(createUrl(port, RequestForwardTestForwarderServlet.class) + "?action=0");
    checkResponse("INVALID REQUEST", url, client);

    url = new URL(createUrl(port, RequestForwardTestForwarderServlet.class) + "?action=s-f-n&target=RequestForwardTest-ForwardeeServlet");
    checkResponse("FORWARD DID NOT GET SESSION", url, client);
  }

  private void checkResponse(String expectedResponse, URL url, HttpClient client) throws ConnectException, IOException {
    System.err.println("=== Send Request [" + (new Date()) + "]: url=[" + url + "]");
    final String actualResponse = HttpUtil.getResponseBody(url, client);
    System.err.println("=== Got Response [" + (new Date()) + "]: url=[" + url + "], response=[" + actualResponse + "]");
    assertTimeDirection();
    assertEquals(expectedResponse, actualResponse);
  }
}
