/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.apache.catalina.connector;


import com.terracotta.session.util.DefaultCookieWriter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class Tomcat55CookieWriter extends DefaultCookieWriter {

  public Tomcat55CookieWriter(boolean isTrackingEnabled, boolean isCookieEnabled, boolean isUrlRewriteEnabled,
                                 String cookieName, String cookieDomain, String cookiePath, String cookieComment,
                                 int cookieMaxAge, boolean isCookieSecure) {
    super(isTrackingEnabled, isCookieEnabled, isUrlRewriteEnabled, cookieName, cookieDomain, cookiePath, cookieComment,
          cookieMaxAge, isCookieSecure);
  }

  protected String computeCookiePath(HttpServletRequest req) {
    // use tomcat's logic for determining the cookie path
    Request internalReq = (Request) req;

    Cookie tmp = new Cookie("invalid", "invalid");
    internalReq.configureSessionCookie(tmp);

    return tmp.getPath();
  }




}
