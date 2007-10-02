/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.GenericServer;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.OkServlet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

/**
 * @author hhuynh
 */
public class WebAppConfigTest extends AbstractDeploymentTest {
  private static final String CONTEXT = "webapptest";
  private static final String MAPPING = "OkServlet";
  private Deployment          deployment;
  private TcConfigBuilder     tcConfigBuilder;

  public static Test suite() {
    return new ServerTestSetup(WebAppConfigTest.class);
  }

  public void setUp() throws Exception {
    super.setUp();
    if (deployment == null) deployment = makeDeployment();
  }

  private Deployment makeDeployment() throws Exception {
    tcConfigBuilder = new TcConfigBuilder();
    tcConfigBuilder.addWebApplication(CONTEXT);
    
    DeploymentBuilder builder = makeDeploymentBuilder(CONTEXT + ".war");
    builder.addServlet(MAPPING, "/ok/*", OkServlet.class, null, false);
    
    // add container specific descriptor
    if (AppServerFactory.getCurrentAppServerId() == AppServerFactory.WEBLOGIC) {
      if (AppServerFactory.getCurrentAppServerMajorVersion().equals("8")) {
        builder.addResourceFullpath("/container-descriptors", "weblogic81.xml", "WEB-INF/weblogic.xml");
      }
      if (AppServerFactory.getCurrentAppServerMajorVersion().equals("9")) {
        builder.addResourceFullpath("/container-descriptors", "weblogic92.xml", "WEB-INF/weblogic.xml");
      }      
    }
    
    return builder.makeDeployment();
  }

  public final void testCookie() throws Exception {
    // server0 is NOT enabled with DSO
    GenericServer.setDsoEnabled(false);
    WebApplicationServer server0 = makeWebApplicationServer(tcConfigBuilder);
    server0.addWarDeployment(deployment, CONTEXT);
    server0.start();

    // server1 is enabled with DSO
    GenericServer.setDsoEnabled(true);
    WebApplicationServer server1 = makeWebApplicationServer(tcConfigBuilder);
    server1.addWarDeployment(deployment, CONTEXT);
    server1.start();

    WebConversation conversation = new WebConversation();
    WebResponse response0 = server0.ping("/" + CONTEXT + "/ok", conversation);
    System.out.println("Cookie from server0 w/o DSO: " + response0.getHeaderField("Set-Cookie"));
    
    conversation = new WebConversation();
    WebResponse response1 = server1.ping("/" + CONTEXT + "/ok", conversation);
    System.out.println("Cookie from server1 w/ DSO: " + response1.getHeaderField("Set-Cookie"));
    
    assertCookie(response0.getHeaderField("Set-Cookie"), response1.getHeaderField("Set-Cookie"));
  }

  private void assertCookie(String expected, String actual) throws Exception {
    SimpleDateFormat formatter = new SimpleDateFormat ("EEE, dd-MMM-yyyy HH:mm:ss z");
    Map expectedCookie = toHash(expected);
    Map actualCookie = toHash(actual);
    
    // all keys are matched
    assertEquals(expectedCookie.keySet(), actualCookie.keySet());
    
    // all content are matched exception [J]SESSIONID
    Set keys = expectedCookie.keySet();
    for (Iterator it = keys.iterator(); it.hasNext(); ) {
      String key = (String)it.next();
      
      // expires time need to match appromixately within 15s of each other
      if (key.equalsIgnoreCase("expires")) {
        Date expectedDate = formatter.parse((String)expectedCookie.get(key));
        Date actualDate = formatter.parse((String)actualCookie.get(key));        
        assertTrue(Math.abs(actualDate.getTime() - expectedDate.getTime()) < 15 * 1000);
      }
      
      if (!key.endsWith("SESSIONID")) {
        assertEquals(expectedCookie.get(key), actualCookie.get(key));
      }
    }
    
  }
  
  private Map toHash(String cookieString) {
    Map map = new HashMap();
    String[] tokens = cookieString.split(";");
    for (int i = 0; i < tokens.length; i++) {
      String[] name_value = tokens[i].trim().split("=");
      if (name_value.length == 2) {
        map.put(name_value[0], name_value[1]);
      } else {
        map.put(name_value[0], "");
      }
    }
    return map;
  }
}
