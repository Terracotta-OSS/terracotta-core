/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

public interface WebAppConfig {

  HttpSessionAttributeListener[] __tc_session_getHttpSessionAttributeListeners();

  HttpSessionListener[] __tc_session_getHttpSessionListener();

  String __tc_session_getCookieDomain();

  String __tc_session_getCookieComment();

  boolean __tc_session_getCookieSecure();

  int __tc_session_getCookieMaxAgeSecs();

  String __tc_session_getCookieName();

  String __tc_session_getCookiePath();

  boolean __tc_session_getCookiesEnabled();

  int __tc_session_getIdLength();

  int __tc_session_getSessionTimeoutSecs();

  boolean __tc_session_getTrackingEnabled();

  boolean __tc_session_getURLRewritingEnabled();

  String __tc_session_getServerId();

  String __tc_session_getSessionDelimiter();

}

