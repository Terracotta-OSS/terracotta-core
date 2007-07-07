/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tctest.webapp.servlets.OkServlet;

import java.net.URL;
import java.util.Date;


public class InstrumentEverythingInContainerTest extends AbstractAppServerTestCase {

  public InstrumentEverythingInContainerTest() {
    if (TestConfigObject.getInstance().appserverFactoryName().equals(NewAppServerFactory.GLASSFISH)) {
      this.disableAllUntil(new Date(Long.MAX_VALUE));
    }
    registerServlet(OkServlet.class);
  }

  protected boolean isSessionTest() {
    return false;
  }

  public void test() throws Exception {
    addInclude("*..*");

    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port = startAppServer(true).serverPort();

    URL url = createUrl(port, OkServlet.class);

    assertEquals("OK", HttpUtil.getResponseBody(url, client));
  }

}
