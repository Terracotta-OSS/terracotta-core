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
import com.tctest.webapp.servlets.SessionIDIntegrityTestServlet;

import java.net.URL;
import java.util.regex.Pattern;


/**
 * Test to make sure session id is preserved with Terracotta
 */
public class SessionIDIntegrityTest extends AbstractAppServerTestCase {

  public SessionIDIntegrityTest() {
    registerServlet(SessionIDIntegrityTestServlet.class);
  }

  public final void testShutdown() throws Exception {

    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();    
    int port1 = startAppServer(true).serverPort();
    int port2 = startAppServer(true).serverPort();

    URL url1 = createUrl(port1, SessionIDIntegrityTestServlet.class, "cmd=insert");
    assertEquals("cmd=insert", "OK", HttpUtil.getResponseBody(url1, client));
    String server0_session_id = extractSessionId(client);
    System.out.println("Server0 session id: " + server0_session_id);
    assertSessionIdIntegrity(server0_session_id, "node-0");

    URL url2 = createUrl(port2, SessionIDIntegrityTestServlet.class, "cmd=query");
    assertEquals("cmd=query", "OK", HttpUtil.getResponseBody(url2, client));
    String server1_session_id = extractSessionId(client);
    System.out.println("Server1 session id: " + server1_session_id);
    assertSessionIdIntegrity(server1_session_id, "node-1");
  }
  
  private void assertSessionIdIntegrity(String sessionId, String extra_id) {
    String factoryName = TestConfigObject.getInstance().appserverFactoryName();

    if (NewAppServerFactory.TOMCAT.equals(factoryName) || 
        NewAppServerFactory.WASCE.equals(factoryName)  ||
        NewAppServerFactory.JBOSS.equals(factoryName)) {
      assertTrue(sessionId.endsWith("." + extra_id));      
    } else if (NewAppServerFactory.WEBLOGIC.equals(factoryName)) {      
      assertTrue(Pattern.matches("\\S+!-?\\d+", sessionId));
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
}
