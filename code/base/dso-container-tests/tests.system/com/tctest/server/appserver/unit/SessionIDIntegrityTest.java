/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Test to make sure session id is preserved with Terracotta
 */
public class SessionIDIntegrityTest extends AbstractAppServerTestCase {

  public SessionIDIntegrityTest() {
    // 
  }

  public final void testShutdown() throws Exception {

    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();    
    int port1 = startAppServer(true).serverPort();
    int port2 = startAppServer(true).serverPort();

    URL url1 = createUrl(port1, TestServlet.class, "cmd=insert");
    assertEquals("cmd=insert", "OK", HttpUtil.getResponseBody(url1, client));
    String server0_session_id = extractSessionId(client);
    System.out.println("Server0 session id: " + server0_session_id);
    assertSessionIdIntegrity(server0_session_id, "node-0");

    URL url2 = createUrl(port2, TestServlet.class, "cmd=query");
    assertEquals("cmd=query", "OK", HttpUtil.getResponseBody(url2, client));
    String server1_session_id = extractSessionId(client);
    System.out.println("Server1 session id: " + server1_session_id);
    assertSessionIdIntegrity(server1_session_id, "node-1");
  }
  
  private void assertSessionIdIntegrity(String sessionId, String extra_id) {
    String factoryName = TestConfigObject.getInstance().appserverFactoryName();

    if (NewAppServerFactory.TOMCAT.equals(factoryName) || 
        NewAppServerFactory.WASCE.equals(factoryName)) {
      assertTrue(sessionId.endsWith("." + extra_id));      
    } else if (NewAppServerFactory.WEBLOGIC.equals(factoryName)) {      
      assertTrue(Pattern.matches("\\S+!-?\\d+", sessionId));
    } else if (NewAppServerFactory.JBOSS.equals(factoryName)) {
      // ~ \S+.jvmroute
    } else if (NewAppServerFactory.WEBSPHERE.equals(factoryName)) {     
      assertTrue(Pattern.matches("0000\\S+:\\S+", sessionId));
    } else {
      throw new RuntimeException("Appserver [" + factoryName + "] is missing in this test");
    }
  }
  
  private String extractSessionId(HttpClient client) {
    Cookie[] cookies = client.getState().getCookies();    
    for (int i = 0; i < cookies.length; i++) {      
      if (cookies[i].getName().equalsIgnoreCase("JSESSIONID")) {
        return cookies[i].getValue();
      }
    }
    return "";
  }

  public static final class TestServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(true);
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      String cmdParam = request.getParameter("cmd");
      if ("insert".equals(cmdParam)) {
        session.setAttribute("hung", "daman");
        out.println("OK");
      } else if ("query".equals(cmdParam)) {
        String data = (String) session.getAttribute("hung");
        if (data != null && data.equals("daman")) {
          out.println("OK");
        } else {
          out.println("ERROR: " + data);
        }
      } else {
        out.println("unknown cmd");
      }

    }
  }
}
