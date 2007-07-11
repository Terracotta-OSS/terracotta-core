/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.tc.object.bytecode.Manager;
import com.tc.properties.TCPropertiesImpl;
import com.terracotta.session.SessionId;

import javax.servlet.http.Cookie;
import javax.servlet.http.MockHttpServletRequest;
import javax.servlet.http.MockHttpServletResponse;

import junit.framework.TestCase;

public class DefaultCookieWriterTest extends TestCase {

  private final String            cookieName = "SomeCookieName";
  private final String            idValue    = "SomeSessionId";

  private DefaultCookieWriter     writer;
  private MockHttpServletRequest  req;
  private MockHttpServletResponse res;
  private SessionId               id;

  public final void setUp() {
    writer = new DefaultCookieWriter(true, true, true, cookieName, null, ConfigProperties.defaultCookiePath, null, -1,
                                     false);
    req = new MockHttpServletRequest();
    res = new MockHttpServletResponse();
    id = new DefaultSessionId(idValue, idValue, idValue, Manager.LOCK_TYPE_WRITE, false);
  }

  public final void testConstructor() {
    // test default c-tor
    DefaultCookieWriter w = DefaultCookieWriter.makeInstance(new ConfigProperties(null, TCPropertiesImpl
        .getProperties()));
    assertEquals(ConfigProperties.defaultCookieName, w.cookieName);

  }

  public final void testCreateCookie() {
    final Cookie c = writer.createCookie(req, id);
    checkCookie(cookieName, idValue, req.getContextPath(), c);
  }

  public final void testWriteCookie() {
    writer.writeCookie(req, res, id);
    Cookie[] cookies = res.getCookies();
    assertNotNull(cookies);
    assertEquals(1, cookies.length);
    checkCookie(cookieName, idValue, req.getContextPath(), cookies[0]);
  }

  public final void testGetCookiePath() {
    // when path specified in c-tor is ConfigProperties.defaultCookiePath, request.getContextPath should be returned
    assertEquals(req.getContextPath(), writer.getCookiePath(req));
    // in case an override is specified it should be used instead
    final String pathOverride = "/SomePath";
    writer = new DefaultCookieWriter(true, true, true, cookieName, null, pathOverride, null, -1, false);
    assertEquals(pathOverride, writer.getCookiePath(req));
  }

  public final void testUrlRewrite() {
    final String requestUrl = "http://localhost:8080/some_page.jsp";
    req.setRequestUrl(requestUrl);
    req.setRequestedSessionId(id.getExternalId());
    req.setSidFromCookie(false);
    req.setSidValid(true);
    req.setScheme("http");
    req.setServerName("localhost");
    req.setServerPort(8080);
    final String actual = writer.encodeRedirectURL("/", req);
    final String expected = "/;" + this.cookieName.toLowerCase() + "=" + id.getExternalId();
    assertEquals(expected, actual);
  }

  private final void checkCookie(final String cName, final String cVal, final String path, Cookie c) {
    assertEquals(cName, c.getName());
    assertEquals(cVal, c.getValue());
    assertEquals(path, c.getPath());
  }

}
