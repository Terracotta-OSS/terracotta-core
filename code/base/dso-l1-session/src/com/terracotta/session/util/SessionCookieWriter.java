/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.SessionId;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface SessionCookieWriter {

  void writeCookie(HttpServletRequest req, HttpServletResponse res, SessionId id);

  String encodeURL(String url, HttpServletRequest req);

  String encodeRedirectURL(String url, HttpServletRequest req);
}
