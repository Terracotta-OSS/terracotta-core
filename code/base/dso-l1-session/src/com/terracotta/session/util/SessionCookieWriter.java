/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.SessionId;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface SessionCookieWriter {

  Cookie writeCookie(HttpServletRequest req, HttpServletResponse res, SessionId id);

  String encodeURL(String url, HttpServletRequest req);

  String encodeRedirectURL(String url, HttpServletRequest req);

  String getCookieName();

  String getPathParameterTag();
}
