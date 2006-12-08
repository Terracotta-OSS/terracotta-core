/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import com.tc.object.bytecode.ManagerUtil;
import com.terracotta.session.WebAppConfig;

import java.util.ArrayList;
import java.util.Properties;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

public class ConfigProperties {

  protected static final String  PREFIX                            = "com.terracotta.session.";
  public static final String     ID_LENGTH                         = PREFIX + "id.length";
  public static final String     SERVER_ID                         = PREFIX + "serverid";
  public static final String     COOKIE_DOMAIN                     = PREFIX + "cookie.domain";
  public static final String     COOKIE_COMMENT                    = PREFIX + "cookie.comment";
  public static final String     COOKIE_SECURE                     = PREFIX + "cookie.secure";
  public static final String     COOKIE_MAX_AGE                    = PREFIX + "cookie.maxage.seconds";
  public static final String     COOKIE_NAME                       = PREFIX + "cookie.name";
  public static final String     COOKIE_PATH                       = PREFIX + "cookie.path";
  public static final String     COOKIE_ENABLED                    = PREFIX + "cookie.enabled";
  public static final String     SESSION_TIMEOUT_SECONDS           = PREFIX + "maxidle.seconds";
  public static final String     TRACKING_ENABLED                  = PREFIX + "tracking.enabled";
  public static final String     URL_REWRITE_ENABLED               = PREFIX + "urlrewrite.enabled";
  public static final String     ATTRIBUTE_LISTENERS               = PREFIX + "attribute.listeners";
  public static final String     SESSION_LISTENERS                 = PREFIX + "listeners";
  public static final String     INVALIDATOR_SLEEP                 = PREFIX + "invalidator.sleep";
  public static final String     REQUEST_BENCHES                   = PREFIX + "request.bench.enabled";
  public static final String     INVALIDATOR_BENCHES               = PREFIX + "invalidator.bench.enabled";

  protected static final boolean defaultCookiesEnabled             = true;
  protected static final boolean defaultTrackingEnabled            = true;
  protected static final boolean defaultUrlEnabled                 = true;
  protected static final String  defaultCookieComment              = null;
  protected static final String  defaultCookieDomain               = null;
  protected static final int     defaultCookieMaxAge               = -1;
  protected static final String  defaultCookieName                 = "JSESSIONID";
  protected static final String  defaultCookiePath                 = null;
  protected static final boolean defaultCookieSecure               = false;
  protected static final int     defaultIdLength                   = 20;
  protected static final String  defaultServerId                   = ManagerUtil.getClientID();
  protected static final int     defaultSessionTimeout             = 30 * 60;
  protected static final int     defaultInvalidatorSleep           = 5 * 60;
  protected static final boolean defaultRequestLogBenchEnabled     = false;
  protected static final boolean defaultInvalidatorLogBenchEnabled = true;

  private final WebAppConfig     wac;
  private final Properties       props;

  public ConfigProperties(final WebAppConfig wac) {
    this(wac, System.getProperties());
  }

  public ConfigProperties(final WebAppConfig wac, final Properties props) {
    Assert.pre(props != null);

    this.wac = wac;
    this.props = props;
  }

  public int getSessionIdLength() {
    final int wacVal = (wac == null) ? -1 : wac.__tc_session_getIdLength();
    final int rv = getIntVal(ID_LENGTH, wacVal, defaultIdLength, -1);
    return Math.max(1, rv);
  }

  public int getCookieMaxAgeSeconds() {
    final int wacVal = (wac == null) ? -2 : wac.__tc_session_getCookieMaxAgeSecs();
    final int rv = getIntVal(COOKIE_MAX_AGE, wacVal, defaultCookieMaxAge, -2);
    return Math.max(-1, rv);
  }

  public int getSessionTimeoutSeconds() {
    final int wacVal = (wac == null) ? -1 : wac.__tc_session_getSessionTimeoutSecs();
    final int rv = getIntVal(SESSION_TIMEOUT_SECONDS, wacVal, defaultSessionTimeout, -1);
    return Math.max(1, rv);
  }

  public int getInvalidatorSleepSeconds() {
    final int wacVal = defaultInvalidatorSleep;
    final int rv = getIntVal(INVALIDATOR_SLEEP, wacVal, defaultInvalidatorSleep, -1);
    return Math.max(1, rv);
  }

  public String getServerId() {
    final String wacVal = wac == null ? null : wac.__tc_session_getServerId();
    return getStringVal(SERVER_ID, wacVal, defaultServerId);
  }

  public String getCookieDomain() {
    final String wacVal = wac == null ? null : wac.__tc_session_getCookieDomain();
    return getStringVal(COOKIE_DOMAIN, wacVal, defaultCookieDomain);
  }

  public String getCookieCoomment() {
    final String wacVal = wac == null ? null : wac.__tc_session_getCookieComment();
    return getStringVal(COOKIE_COMMENT, wacVal, defaultCookieComment);
  }

  public String getCookieName() {
    final String wacVal = wac == null ? null : wac.__tc_session_getCookieName();
    return getStringVal(COOKIE_NAME, wacVal, defaultCookieName);
  }

  public String getCookiePath() {
    final String wacVal = wac == null ? null : wac.__tc_session_getCookiePath();
    return getStringVal(COOKIE_PATH, wacVal, defaultCookiePath);
  }

  public boolean getCookieSecure() {
    final String wacVal = wac == null ? null : Boolean.toString(wac.__tc_session_getCookieSecure());
    final String boolVal = getStringVal(COOKIE_SECURE, wacVal, Boolean.toString(defaultCookieSecure));
    return "true".equals(boolVal);
  }

  public boolean getCookiesEnabled() {
    final String wacVal = wac == null ? null : Boolean.toString(wac.__tc_session_getCookiesEnabled());
    final String boolVal = getStringVal(COOKIE_ENABLED, wacVal, Boolean.toString(defaultCookiesEnabled));
    return "true".equals(boolVal);
  }

  public boolean getSessionTrackingEnabled() {
    final String wacVal = wac == null ? null : Boolean.toString(wac.__tc_session_getTrackingEnabled());
    final String boolVal = getStringVal(TRACKING_ENABLED, wacVal, Boolean.toString(defaultTrackingEnabled));
    return "true".equals(boolVal);
  }

  public boolean getUrlRewritingEnabled() {
    final String wacVal = wac == null ? null : Boolean.toString(wac.__tc_session_getURLRewritingEnabled());
    final String boolVal = getStringVal(URL_REWRITE_ENABLED, wacVal, Boolean.toString(defaultUrlEnabled));
    return "true".equals(boolVal);
  }

  public boolean getRequestLogBenchEnabled() {
    final String boolVal = getStringVal(REQUEST_BENCHES, null, Boolean.toString(defaultRequestLogBenchEnabled));
    return "true".equals(boolVal);
  }

  public boolean getInvalidatorLogBenchEnabled() {
    final String boolVal = getStringVal(INVALIDATOR_BENCHES, null, Boolean.toString(defaultInvalidatorLogBenchEnabled));
    return "true".equals(boolVal);
  }

  public HttpSessionAttributeListener[] getSessionAttributeListeners() {
    final String list = getProperty(ATTRIBUTE_LISTENERS);
    if (list == null) return (wac == null) ? null : wac.__tc_session_getHttpSessionAttributeListeners();
    final String[] types = list.split(",");
    if (types == null || types.length == 0) return null;
    ArrayList listeners = new ArrayList();
    for (int i = 0; i < types.length; i++) {
      HttpSessionAttributeListener l = makeAttributeListener(types[i]);
      if (l != null) listeners.add(l);
    }
    final HttpSessionAttributeListener[] rv = new HttpSessionAttributeListener[listeners.size()];
    listeners.toArray(rv);
    return rv;
  }

  public HttpSessionListener[] getSessionListeners() {
    final String list = getProperty(SESSION_LISTENERS);
    if (list == null) return (wac == null) ? null : wac.__tc_session_getHttpSessionListener();
    final String[] types = list.split(",");
    if (types == null || types.length == 0) return null;
    ArrayList listeners = new ArrayList();
    for (int i = 0; i < types.length; i++) {
      HttpSessionListener l = makeSessionListener(types[i]);
      if (l != null) listeners.add(l);
    }
    final HttpSessionListener[] rv = new HttpSessionListener[listeners.size()];
    listeners.toArray(rv);
    return rv;
  }

  protected static HttpSessionAttributeListener makeAttributeListener(String type) {
    type = type.trim();
    HttpSessionAttributeListener rv = null;
    try {
      final Class c = Class.forName(type);
      if (HttpSessionAttributeListener.class.isAssignableFrom(c)) rv = (HttpSessionAttributeListener) c.newInstance();
    } catch (Exception e) {
      // TODO: log message here...
      return null;
    }
    return rv;
  }

  protected static HttpSessionListener makeSessionListener(String type) {
    type = type.trim();
    HttpSessionListener rv = null;
    try {
      final Class c = Class.forName(type);
      if (HttpSessionListener.class.isAssignableFrom(c)) rv = (HttpSessionListener) c.newInstance();
    } catch (Exception e) {
      // TODO: log message here...
      return null;
    }
    return rv;
  }

  protected String getStringVal(final String propName, final String wacVal, final String defVal) {
    String spcVal = getProperty(propName);
    if (spcVal != null) return spcVal;
    if (wacVal != null) return wacVal;
    return defVal;
  }

  protected int getIntVal(String propName, int wacVal, int defVal, int invalidVal) {
    final int spcVal = getPropertyInt(propName, invalidVal);
    if (spcVal != invalidVal) return spcVal;
    if (wacVal != invalidVal) return wacVal;
    return defVal;
  }

  protected String getProperty(final String propName) {
    String rv = props.getProperty(propName);
    if (rv == null) return null;
    rv = rv.trim();
    if (rv.length() == 0) return null;
    else return rv;
  }

  protected int getPropertyInt(String propName, int defVal) {
    final String val = props.getProperty(propName);
    if (val == null || val.length() == 0) return defVal;
    try {
      return Integer.parseInt(val);
    } catch (Exception e) {
      return defVal;
    }
  }
}
