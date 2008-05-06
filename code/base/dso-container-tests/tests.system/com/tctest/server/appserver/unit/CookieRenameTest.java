/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.Assert;
import com.tctest.webapp.servlets.CookieRenameServlet;
import com.terracotta.session.util.ConfigProperties;

import java.util.Arrays;
import java.util.List;

import junit.framework.Test;

/**
 * Test for CDV-544
 *
 * @author hhuynh
 */
public class CookieRenameTest extends AbstractOneServerDeploymentTest {
  private static final String CONTEXT        = "CookieRenameTest";
  private static final String SERVLET        = "CookieRenameServlet";

  private static final String RENAMED_COOKIE = "MY_SESSION_ID";

  public CookieRenameTest() {
    //
  }

  public static Test suite() {
    return new CookieRenameTestSetup();
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return request(server, "", params, con);
  }

  private WebResponse request(WebApplicationServer server, String pathParam, String params, WebConversation con)
      throws Exception {
    return server.ping("/" + CONTEXT + "/" + SERVLET + pathParam + "?" + params, con);
  }

  public void test() throws Exception {
    basicCookieTest();
    standardCookieNameTest();
    multipleCookiesTest();
    multipleCookiesTest2();

    basicUrlTest();
    standardNameUrlTest();
    mixedUrlTest();
  }

  private void mixedUrlTest() throws Exception {
    String pathParam = basicUrlSetup();

    // tack on another path parameter
    pathParam = pathParam + ";jsessionid=666";

    WebConversation wc = new WebConversation(); // new (no cookies)!
    WebResponse wr = request(server0, pathParam, "cmd=query&source=url", wc);
    assertEquals("OK", wr.getText().trim());
  }

  private void standardCookieNameTest() throws Exception {
    WebConversation wc = basicCookieSetup();
    String sessionId = getSessionId(wc);

    wc = new WebConversation(); // clean -- no cookies
    wc.putCookie("JSESSIONID", sessionId);

    // since the "wrong" cookie name is being used, the requested id on the request will be null and no source
    WebResponse wr = request(server0, "cmd=no-session&requested-id=null&source=none", wc);
    assertEquals("OK", wr.getText().trim());
  }

  private void standardNameUrlTest() throws Exception {
    String pathParam = basicUrlSetup();
    String id = pathParam.substring(pathParam.indexOf('='));

    WebConversation wc = new WebConversation(); // new (no cookies)!

    // use the standard parameter (jsessionid) and with the real session, but we shouldn't find a session
    WebResponse wr = request(server0, ";jessionid=" + id, "cmd=no-session&requested-id=null&source=none", wc);
    assertEquals("OK", wr.getText().trim());
  }

  private void basicUrlTest() throws Exception {
    String pathParam = basicUrlSetup();
    WebConversation wc = new WebConversation(); // new (no cookies)!
    WebResponse wr = request(server0, pathParam, "cmd=query&source=url", wc);
    assertEquals("OK", wr.getText().trim());
  }

  private void multipleCookiesTest() throws Exception {
    WebConversation wc = basicCookieSetup();

    // add a cookie at path "/" with bogus sessionID
    WebResponse wr = request(server0, "cmd=add-cookie&add-cookie-name=" + RENAMED_COOKIE
                                      + "&add-cookie-path=/&add-cookie-value=666", wc);
    assertEquals("OK", wr.getText().trim());

    // we shouldn't find any data, and requested session ID should be 666
    wr = request(server0, "cmd=no-session&requested-id=666&source=cookie", wc);
    assertEquals("OK", wr.getText().trim());
  }

  private void multipleCookiesTest2() throws Exception {
    WebConversation wc = basicCookieSetup();

    String sessionId = getSessionId(wc);

    // add a cookie at path "/CookieRenameTest" with bogus sessionID
    WebResponse wr = request(server0, "cmd=add-cookie&add-cookie-name=" + RENAMED_COOKIE
                                      + "&add-cookie-path=/CookieRenameTest&add-cookie-value=666", wc);
    assertEquals("OK", wr.getText().trim());

    // add a cookie at path "/" with real sessionID
    wr = request(server0, "cmd=add-cookie&add-cookie-name=" + RENAMED_COOKIE + "&add-cookie-path=/&add-cookie-value="
                          + sessionId, wc);
    assertEquals("OK", wr.getText().trim());

    // this time we should find the session data
    wr = request(server0, "cmd=query&source=cookie", wc);
    assertEquals("OK", wr.getText().trim());
  }

  private String getSessionId(WebConversation wc) {
    return wc.getCookieValue(RENAMED_COOKIE);
  }

  private void basicCookieTest() throws Exception {
    basicCookieSetup();
  }

  WebConversation basicCookieSetup() throws Exception {
    WebConversation wc = new WebConversation();
    WebResponse wr = request(server0, "cmd=insert", wc);
    assertEquals("OK", wr.getText().trim());
    List names = Arrays.asList(wc.getCookieNames());
    System.out.println("Cookie names: " + names);
    assertTrue(names.contains(RENAMED_COOKIE));
    wr = request(server0, "cmd=query&source=cookie", wc);
    assertEquals("OK", wr.getText().trim());
    return wc;
  }

  String basicUrlSetup() throws Exception {
    WebConversation wc = new WebConversation();
    WebResponse wr = request(server0, "cmd=encode-url", wc);
    String url = wr.getText().trim();
    Assert.eval(url, url.indexOf(";" + RENAMED_COOKIE.toLowerCase() + "=") >= 0);
    return url.substring(url.indexOf(';'));
  }

  private static class CookieRenameTestSetup extends OneServerTestSetup {

    public CookieRenameTestSetup() {
      super(CookieRenameTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(SERVLET, "/" + SERVLET + "/*", CookieRenameServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplication(CONTEXT);
    }

    protected void configureServerParamers(StandardAppServerParameters params) {
      params.appendJvmArgs("-Dcom.tc." + ConfigProperties.COOKIE_NAME + "=" + RENAMED_COOKIE);
      params.appendJvmArgs("-Dcom.tc." + ConfigProperties.URL_REWRITE_ENABLED + "=true");
    }

  }
}
