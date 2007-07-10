/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tctest.webapp.servlets.OkServlet;

import java.net.URL;


public class InstrumentEverythingInContainerTest extends AbstractAppServerTestCase {

  public InstrumentEverythingInContainerTest() {
    registerServlet(OkServlet.class);
  }

  protected boolean isSessionTest() {
    return false;
  }

  public void test() throws Exception {
    addInclude("*..*");

    // These bytes are obfuscated and get verify errors when instrumented by DSO
    addExclude("com.sun.crypto.provider..*");

    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port = startAppServer(true).serverPort();

    URL url = createUrl(port, OkServlet.class);

    assertEquals("OK", HttpUtil.getResponseBody(url, client));
  }
}
