/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.io.IOUtils;

import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.GenericServer;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.appserver.was6x.Was6xAppServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.OkServlet;

import java.io.File;
import java.io.FileOutputStream;
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
  private static final String CONTEXT = "WebAppConfigTest";
  private static final String MAPPING = "OkServlet";
  private Deployment          deployment;
  private TcConfigBuilder     tcConfigBuilder;
  
  public WebAppConfigTest() {
    // DEV-984
    if (AppServerFactory.getCurrentAppServerId() == AppServerFactory.JBOSS) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

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
    builder.addSessionConfig("session-timeout", "69");

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
  
  public void testAppConfig() throws Exception {    
    // server0 is NOT enabled with DSO
    GenericServer.setDsoEnabled(false);
    WebApplicationServer server0 = makeWebApplicationServer(tcConfigBuilder);
    server0.addWarDeployment(deployment, CONTEXT);
    setCookieForWebsphere(server0);
    server0.start();

    // server1 is enabled with DSO
    GenericServer.setDsoEnabled(true);
    WebApplicationServer server1 = makeWebApplicationServer(tcConfigBuilder);
    server1.addWarDeployment(deployment, CONTEXT);
    setCookieForWebsphere(server1);
    server1.start();

    // test cookie settings
    WebResponse response0 = server0.ping("/" + CONTEXT + "/ok");
    System.out.println("Cookie from server0 w/o DSO: " + response0.getHeaderField("Set-Cookie"));
    
    WebResponse response1 = server1.ping("/" + CONTEXT + "/ok");
    System.out.println("Cookie from server1 w/ DSO: " + response1.getHeaderField("Set-Cookie"));

    assertCookie(response0.getHeaderField("Set-Cookie"), response1.getHeaderField("Set-Cookie"));
    
    // test session-timeout, it is set at 69 minutes
    response1 = server1.ping("/" + CONTEXT + "/ok?cmd=getMaxInactiveInterval");
    assertEquals("4140", response1.getText().trim()); // 69min * 60 = 4140sec
  }

  private void setCookieForWebsphere(WebApplicationServer server) throws Exception {
    if (AppServerFactory.getCurrentAppServerId() == AppServerFactory.WEBSPHERE) {
      System.out.println("Setting cookie for websphere...");
      File cookieSettingsScript = File.createTempFile("cookiesettings", ".py");
      cookieSettingsScript.deleteOnExit();
      FileOutputStream out = new FileOutputStream(cookieSettingsScript);
      IOUtils.copy(getClass().getResourceAsStream("/com/tc/test/server/appserver/was6x/cookiesettings.py"), out);
      out.close();
      Was6xAppServer wasServer = (Was6xAppServer) ((GenericServer) server).getAppServer();
      wasServer.setExtraScript(cookieSettingsScript);
    }
  }

  private void assertCookie(String expected, String actual) throws Exception {
    SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    Map expectedCookie = toHash(expected);
    Map actualCookie = toHash(actual);

    // all keys are matched
    assertEquals(expectedCookie.keySet(), actualCookie.keySet());

    // all content are matched exception [J]SESSIONID
    Set keys = expectedCookie.keySet();
    for (Iterator it = keys.iterator(); it.hasNext();) {
      String key = (String) it.next();

      // expires time need to match appromixately within 15s of each other
      if (key.equalsIgnoreCase("expires")) {
        // websphere doesn't use "-" between date when the other webapps do
        // strip them out to be consistent
        Date expectedDate = formatter.parse(((String) expectedCookie.get(key)).replace('-', ' '));
        Date actualDate = formatter.parse(((String) actualCookie.get(key)).replace('-', ' '));
        assertTrue(Math.abs(actualDate.getTime() - expectedDate.getTime()) < 15 * 1000);
        continue;
      }

      if (key.toUpperCase().endsWith("SESSIONID")) {
        switch (AppServerFactory.getCurrentAppServerId()) {
          case AppServerFactory.WEBLOGIC:
          case AppServerFactory.WEBSPHERE:
            assertEquals(key.toUpperCase(), "CUSTOMSESSIONID");
            break;
          default:
            assertEquals(key.toUpperCase(), "JSESSIONID");
        }
      } else {
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
