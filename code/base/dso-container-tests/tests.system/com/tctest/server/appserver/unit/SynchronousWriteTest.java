/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tctest.webapp.servlets.SynchronousWriteTestServlet;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;


/**
 * Simplest test case for DSO. This class should be used as a model for building container based tests. A feature which
 * was omitted in this test is the overloaded startAppServer() method which also takes a properties file. These
 * properties will then be available to the innerclass servlet below as system properties. View the superclass
 * description for more information about general usage.
 */
public class SynchronousWriteTest extends AbstractAppServerTestCase {

  private static final int INTENSITY = 100;

  public SynchronousWriteTest() {
    registerServlet(SynchronousWriteTestServlet.class);
  }

  public final void testSessions() throws Exception {
    setSynchronousWrite(true);
    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port0 = startAppServer(true).serverPort();
    int port1 = startAppServer(true).serverPort();

    createTransactions(client, port0, SynchronousWriteTestServlet.class, "server=0");
    URL url1 = new URL(createUrl(port1, SynchronousWriteTestServlet.class) + "?server=1&data="
                       + (INTENSITY - 1));

    assertEquals("99", HttpUtil.getResponseBody(url1, client));
  }

  private void createTransactions(HttpClient client, int port, Class klass, String params) throws Exception {
    File dataFile = new File(this.getTempDirectory(), "synchwrite.txt");
    PrintWriter out = new PrintWriter(new FileWriter(dataFile));

    for (int i = 0; i < INTENSITY; i++) {
      out.println("data" + i + "=" + i);
      URL url0 = new URL(createUrl(port, klass) + "?" + params + "&data=" + i);
      assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    }

    out.close();
  }
}
