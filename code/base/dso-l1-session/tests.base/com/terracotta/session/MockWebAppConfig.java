/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

public class MockWebAppConfig implements WebAppConfig {

  private String                         cookieComment;
  private String                         cookieDomain;
  private String                         cookieName;
  private String                         cookiePath;
  private String                         serverId;
  private int                            cookieMaxAge;
  private int                            idLength;
  private int                            timeoutSeconds;
  private boolean                        cookieSecure;
  private boolean                        cookieEnabled;
  private boolean                        urlEnabled;
  private boolean                        trackingEnabled;
  private HttpSessionAttributeListener[] attrListeners;
  private HttpSessionListener[]          sessListeners;

  public String __tc_session_getCookieComment() {
    return cookieComment;
  }

  public String __tc_session_getCookieDomain() {
    return cookieDomain;
  }

  public int __tc_session_getCookieMaxAgeSecs() {
    return cookieMaxAge;
  }

  public String __tc_session_getCookieName() {
    return cookieName;
  }

  public String __tc_session_getCookiePath() {
    return cookiePath;
  }

  public boolean __tc_session_getCookieSecure() {
    return cookieSecure;
  }

  public boolean __tc_session_getCookiesEnabled() {
    return cookieEnabled;
  }

  public HttpSessionAttributeListener[] __tc_session_getHttpSessionAttributeListeners() {
    return attrListeners;
  }

  public HttpSessionListener[] __tc_session_getHttpSessionListener() {
    return sessListeners;
  }

  public int __tc_session_getIdLength() {
    return idLength;
  }

  public String __tc_session_getServerId() {
    return serverId;
  }

  public int __tc_session_getSessionTimeoutSecs() {
    return timeoutSeconds;
  }

  public boolean __tc_session_getTrackingEnabled() {
    return trackingEnabled;
  }

  public boolean __tc_session_getURLRewritingEnabled() {
    return urlEnabled;
  }

  public void setAttrListeners(HttpSessionAttributeListener[] attrListeners) {
    this.attrListeners = attrListeners;
  }

  public void setCookieComment(String cookieComment) {
    this.cookieComment = cookieComment;
  }

  public void setCookieDomain(String cookieDomain) {
    this.cookieDomain = cookieDomain;
  }

  public void setCookieEnabled(boolean cookieEnabled) {
    this.cookieEnabled = cookieEnabled;
  }

  public void setCookieMaxAge(int cookieMaxAge) {
    this.cookieMaxAge = cookieMaxAge;
  }

  public void setCookieName(String cookieName) {
    this.cookieName = cookieName;
  }

  public void setCookiePath(String cookiePath) {
    this.cookiePath = cookiePath;
  }

  public void setCookieSecure(boolean cookieSecure) {
    this.cookieSecure = cookieSecure;
  }

  public void setIdLength(int idLength) {
    this.idLength = idLength;
  }

  public void setServerId(String serverId) {
    this.serverId = serverId;
  }

  public void setSessListeners(HttpSessionListener[] sessListeners) {
    this.sessListeners = sessListeners;
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  public void setTrackingEnabled(boolean trackingEnabled) {
    this.trackingEnabled = trackingEnabled;
  }

  public void setUrlEnabled(boolean urlEnabled) {
    this.urlEnabled = urlEnabled;
  }

}
