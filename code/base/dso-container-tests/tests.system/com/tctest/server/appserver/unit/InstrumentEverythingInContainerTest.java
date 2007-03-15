/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class InstrumentEverythingInContainerTest extends AbstractAppServerTestCase {

  public void test() throws Exception {
    addInclude("*..*");

    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port = startAppServer(true).serverPort();

    URL url = createUrl(port, TestServlet.class);

    assertEquals("OK", HttpUtil.getResponseBody(url, client));
  }

  public static final class TestServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(true);
      session.setAttribute("da", "bomb");
      response.setContentType("text/html");
      response.getWriter().println("OK");
    }
  }

}
