/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.WebAppConfig;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

public class DefaultWebAppConfig implements WebAppConfig {

  private final int sessTimeoutSeconds;
  private final HttpSessionAttributeListener[] attrList;
  private final HttpSessionListener[]          sessList;

  public DefaultWebAppConfig(final int sessTimeoutSeconds, final HttpSessionAttributeListener[] attrList,  final HttpSessionListener[] sessList) {
    this.sessTimeoutSeconds = sessTimeoutSeconds;
    this.attrList = attrList;
    this.sessList = sessList;
  }

  public String __tc_session_getCookieComment() {
    return null;
  }

  public String __tc_session_getCookieDomain() {
    return null;
  }

  public int __tc_session_getCookieMaxAgeSecs() {
    return -1;
  }

  public String __tc_session_getCookieName() {
    return null;
  }

  public String __tc_session_getCookiePath() {
    return null;
  }

  public boolean __tc_session_getCookieSecure() {
    return false;
  }

  public boolean __tc_session_getCookiesEnabled() {
    return true;
  }

  public HttpSessionAttributeListener[] __tc_session_getHttpSessionAttributeListeners() {
    return attrList;
  }

  public HttpSessionListener[] __tc_session_getHttpSessionListener() {
    return sessList;
  }

  public int __tc_session_getIdLength() {
    return -1;
  }

  public String __tc_session_getServerId() {
    return null;
  }

  public int __tc_session_getSessionTimeoutSecs() {
    return sessTimeoutSeconds;
  }

  public boolean __tc_session_getTrackingEnabled() {
    return true;
  }

  public boolean __tc_session_getURLRewritingEnabled() {
    return true;
  }

}
