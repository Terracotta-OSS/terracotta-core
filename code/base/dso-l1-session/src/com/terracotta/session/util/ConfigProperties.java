/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.tc.logging.TCLogger;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.properties.TCProperties;
import com.terracotta.session.WebAppConfig;

import java.util.ArrayList;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

public class ConfigProperties {

  protected static final String PREFIX                     = "session.";
  public static final String    ID_LENGTH                  = PREFIX + "id.length";
  public static final String    SERVER_ID                  = PREFIX + "serverid";
  public static final String    COOKIE_DOMAIN              = PREFIX + "cookie.domain";
  public static final String    COOKIE_COMMENT             = PREFIX + "cookie.comment";
  public static final String    COOKIE_SECURE              = PREFIX + "cookie.secure";
  public static final String    COOKIE_MAX_AGE             = PREFIX + "cookie.maxage.seconds";
  public static final String    COOKIE_NAME                = PREFIX + "cookie.name";
  public static final String    COOKIE_PATH                = PREFIX + "cookie.path";
  public static final String    COOKIE_ENABLED             = PREFIX + "cookie.enabled";
  public static final String    SESSION_TIMEOUT_SECONDS    = PREFIX + "maxidle.seconds";
  public static final String    TRACKING_ENABLED           = PREFIX + "tracking.enabled";
  public static final String    URL_REWRITE_ENABLED        = PREFIX + "urlrewrite.enabled";
  public static final String    ATTRIBUTE_LISTENERS        = PREFIX + "attribute.listeners";
  public static final String    SESSION_LISTENERS          = PREFIX + "listeners";
  public static final String    INVALIDATOR_SLEEP          = PREFIX + "invalidator.sleep";
  public static final String    REQUEST_BENCHES            = PREFIX + "request.bench.enabled";
  public static final String    INVALIDATOR_BENCHES        = PREFIX + "invalidator.bench.enabled";
  public static final String    STUCK_REQUEST_TRACKING     = PREFIX + "request.tracking";
  public static final String    STUCK_REQUEST_THREAD_DUMP  = PREFIX + "request.tracking.dump";
  public static final String    STUCK_REQUEST_INTERVAL     = PREFIX + "request.tracking.interval";
  public static final String    STUCK_REQUEST_THRESHOLD    = PREFIX + "request.tracking.threshold";
  public static final String    DEBUG_SERVER_HOPS          = PREFIX + "debug.hops";
  public static final String    DEBUG_SERVER_HOPS_INTERVAL = PREFIX + "debug.hops.interval";
  public static final String    DEBUG_SESSION_INVALIDATE   = PREFIX + "debug.invalidate";
  public static final String    DELIMITER                  = PREFIX + "delimiter";
  public static final String    DEBUG_SESSIONS             = PREFIX + "debug.sessions";

  public static final boolean   defaultCookiesEnabled      = true;
  public static final boolean   defaultTrackingEnabled     = true;
  public static final boolean   defaultUrlEnabled          = true;
  public static final String    defaultCookieComment       = null;
  public static final String    defaultCookieDomain        = null;
  public static final int       defaultCookieMaxAge        = -1;
  public static final String    defaultCookieName          = "JSESSIONID";
  public static final String    defaultCookiePath          = "/";
  public static final boolean   defaultCookieSecure        = false;
  public static final int       defaultIdLength            = 20;
  public static final String    defaultServerId            = ManagerUtil.getClientID();
  public static final int       defaultSessionTimeout      = 30 * 60;
  public static final String    defaultDelimiter           = "!";

  private final WebAppConfig    wac;
  private final TCProperties    props;
  private final ClassLoader     loader;
  private final TCLogger        logger;

  public ConfigProperties(final WebAppConfig wac, ClassLoader loader) {
    this(wac, ManagerUtil.getTCProperties(), loader, ManagerUtil.getLogger("com.tc.session.ConfigProperties"));
  }

  public ConfigProperties(final WebAppConfig wac, final TCProperties props, TCLogger logger) {
    this(wac, props, ConfigProperties.class.getClassLoader(), logger);
  }

  private ConfigProperties(final WebAppConfig wac, final TCProperties props, ClassLoader loader, TCLogger logger) {
    Assert.pre(props != null);

    this.wac = wac;
    this.props = props;
    this.loader = loader;
    this.logger = logger;
  }

  public int getDebugServerHopsInterval() {
    return props.getInt(DEBUG_SERVER_HOPS_INTERVAL);
  }

  public boolean isDebugSessionInvalidate() {
    return props.getBoolean(DEBUG_SESSION_INVALIDATE);
  }

  public boolean isDebugServerHops() {
    return props.getBoolean(DEBUG_SERVER_HOPS);
  }

  public boolean isRequestTrackingEnabled() {
    return props.getBoolean(STUCK_REQUEST_TRACKING);
  }

  public boolean isDumpThreadsOnStuckRequests() {
    return props.getBoolean(STUCK_REQUEST_THREAD_DUMP);
  }

  public long getRequestTrackerSleepMillis() {
    return props.getLong(STUCK_REQUEST_INTERVAL);
  }

  public long getRequestTrackerStuckThresholdMillis() {
    return props.getLong(STUCK_REQUEST_THRESHOLD);
  }

  public int getSessionIdLength() {
    String spcVal = getProperty(ID_LENGTH);
    if (spcVal != null) { return Integer.parseInt(spcVal); }

    if (wac != null) { return wac.__tc_session_getIdLength(); }

    return defaultIdLength;
  }

  public int getCookieMaxAgeSeconds() {
    String spcVal = getProperty(COOKIE_MAX_AGE);
    if (spcVal != null) { return Integer.parseInt(spcVal); }

    if (wac != null) { return wac.__tc_session_getCookieMaxAgeSecs(); }

    return defaultCookieMaxAge;
  }

  public int getSessionTimeoutSeconds() {
    String spcVal = getProperty(SESSION_TIMEOUT_SECONDS);
    if (spcVal != null) { return Integer.parseInt(spcVal); }

    if (wac != null) { return wac.__tc_session_getSessionTimeoutSecs(); }

    return defaultSessionTimeout;
  }

  public int getInvalidatorSleepSeconds() {
    return props.getInt(INVALIDATOR_SLEEP);
  }

  public String getServerId() {
    final String wacVal = wac == null ? null : wac.__tc_session_getServerId();
    return getStringVal(SERVER_ID, wacVal, defaultServerId);
  }

  public String getDelimiter() {
    final String wacVal = wac == null ? null : wac.__tc_session_getSessionDelimiter();
    return getStringVal(DELIMITER, wacVal, defaultDelimiter);
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
    return getStringVal(COOKIE_PATH, wacVal, null);
  }

  public boolean getCookieSecure() {
    final String wacVal = wac == null ? null : Boolean.toString(wac.__tc_session_getCookieSecure());
    final String boolVal = getStringVal(COOKIE_SECURE, wacVal, Boolean.toString(defaultCookieSecure));
    return Boolean.valueOf(boolVal).booleanValue();
  }

  public boolean getCookiesEnabled() {
    final String wacVal = wac == null ? null : Boolean.toString(wac.__tc_session_getCookiesEnabled());
    final String boolVal = getStringVal(COOKIE_ENABLED, wacVal, Boolean.toString(defaultCookiesEnabled));
    return Boolean.valueOf(boolVal).booleanValue();
  }

  public boolean getSessionTrackingEnabled() {
    final String wacVal = wac == null ? null : Boolean.toString(wac.__tc_session_getTrackingEnabled());
    final String boolVal = getStringVal(TRACKING_ENABLED, wacVal, Boolean.toString(defaultTrackingEnabled));
    return Boolean.valueOf(boolVal).booleanValue();
  }

  public boolean getUrlRewritingEnabled() {
    final String wacVal = wac == null ? null : Boolean.toString(wac.__tc_session_getURLRewritingEnabled());
    final String boolVal = getStringVal(URL_REWRITE_ENABLED, wacVal, Boolean.toString(defaultUrlEnabled));
    return Boolean.valueOf(boolVal).booleanValue();
  }

  public boolean isDebugSessions() {
    return props.getBoolean(DEBUG_SESSIONS);
  }

  public boolean getRequestLogBenchEnabled() {
    return props.getBoolean(REQUEST_BENCHES);
  }

  public boolean getInvalidatorLogBenchEnabled() {
    return props.getBoolean(INVALIDATOR_BENCHES);
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

  protected HttpSessionAttributeListener makeAttributeListener(String type) {
    type = type.trim();
    HttpSessionAttributeListener rv = null;
    try {
      final Class c = Class.forName(type, true, loader);
      if (HttpSessionAttributeListener.class.isAssignableFrom(c)) rv = (HttpSessionAttributeListener) c.newInstance();
    } catch (Exception e) {
      logger.error("Error instantiaing listener of type " + type, e);
      return null;
    }
    return rv;
  }

  protected HttpSessionListener makeSessionListener(String type) {
    type = type.trim();
    HttpSessionListener rv = null;
    try {
      final Class c = Class.forName(type, true, loader);
      if (HttpSessionListener.class.isAssignableFrom(c)) rv = (HttpSessionListener) c.newInstance();
    } catch (Exception e) {
      logger.error("Error instantiaing listener of type " + type, e);
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

  protected String getProperty(final String propName) {
    String rv = props.getProperty(propName, true);
    if (rv == null) return null;
    rv = rv.trim();
    if (rv.length() == 0) return null;
    else return rv;
  }

}
