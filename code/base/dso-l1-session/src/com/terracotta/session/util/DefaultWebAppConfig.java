/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.WebAppConfig;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

public class DefaultWebAppConfig implements WebAppConfig {

  private final int                            sessTimeoutSeconds;
  private final HttpSessionAttributeListener[] attrList;
  private final HttpSessionListener[]          sessList;
  private final String                         serverId;
  private final String                         delimiter;

  public DefaultWebAppConfig(final int sessTimeoutSeconds, final HttpSessionAttributeListener[] attrList,
                             final HttpSessionListener[] sessList, final String delimiter, final String serverId) {
    this.sessTimeoutSeconds = sessTimeoutSeconds;
    this.attrList = attrList;
    this.sessList = sessList;
    this.delimiter = delimiter;
    this.serverId = serverId;
  }

  public String __tc_session_getCookieComment() {
    return null;
  }

  public String __tc_session_getCookieDomain() {
    return null;
  }

  public int __tc_session_getCookieMaxAgeSecs() {
    return ConfigProperties.defaultCookieMaxAge;
  }

  public String __tc_session_getCookieName() {
    return null;
  }

  public String __tc_session_getCookiePath() {
    return null;
  }

  public boolean __tc_session_getCookieSecure() {
    return ConfigProperties.defaultCookieSecure;
  }

  public boolean __tc_session_getCookiesEnabled() {
    return ConfigProperties.defaultCookiesEnabled;
  }

  public HttpSessionAttributeListener[] __tc_session_getHttpSessionAttributeListeners() {
    return attrList;
  }

  public HttpSessionListener[] __tc_session_getHttpSessionListener() {
    return sessList;
  }

  public int __tc_session_getIdLength() {
    return ConfigProperties.defaultIdLength;
  }

  public String __tc_session_getServerId() {
    return serverId;
  }

  public int __tc_session_getSessionTimeoutSecs() {
    return sessTimeoutSeconds;
  }

  public boolean __tc_session_getTrackingEnabled() {
    return ConfigProperties.defaultTrackingEnabled;
  }

  public boolean __tc_session_getURLRewritingEnabled() {
    return ConfigProperties.defaultUrlEnabled;
  }

  public String __tc_session_getSessionDelimiter() {
    return delimiter;
  }

}
