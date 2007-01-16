/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.tc.exception.ImplementMe;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.terracotta.session.MockWebAppConfig;

import java.util.Properties;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import junit.framework.TestCase;

public class ConfigPropertiesTest extends TestCase {

  private final String     wacCookieComment   = "wacCookieComment";
  private final String     wacCookieDomain    = "wacCookieDomain";
  private final String     wacCookieName      = "wacCookieName";
  private final String     wacCookiePath      = "wacCookiePath";
  private final String     wacServerId        = "wacServerId";
  private final int        wacCookieMaxAge    = 111;
  private final int        wacIdLength        = 333;
  private final int        wacTimeout         = 555;
  private final int        wacInvalidatorSec  = 5 * 60;
  private final boolean    wacCookieEnabled   = true;
  private final boolean    wacTrackingEnabled = true;
  private final boolean    wacUrlEnabled      = true;
  private final boolean    wacCookieSecure    = true;

  private final String     spcCookieComment   = "spcCookieComment";
  private final String     spcCookieDomain    = "spcCookieDomain";
  private final String     spcCookieName      = "spcCookieName";
  private final String     spcCookiePath      = "spcCookiePath";
  private final String     spcServerId        = "spcServerId";
  private final int        spcCookieMaxAge    = 222;
  private final int        spcIdLength        = 444;
  private final int        spcTimeout         = 666;
  private final int        spcInvalidatorSec  = 999;
  private final boolean    spcCookieEnabled   = false;
  private final boolean    spcTrackingEnabled = false;
  private final boolean    spcUrlEnabled      = false;
  private final boolean    spcCookieSecure    = false;

  private MockWebAppConfig wac;
  private TCProperties     tpc;

  protected void setUp() throws Exception {
    super.setUp();

    wac = new MockWebAppConfig();
    wac.setCookieComment(wacCookieComment);
    wac.setCookieDomain(wacCookieDomain);
    wac.setCookieEnabled(wacCookieEnabled);
    wac.setCookieMaxAge(wacCookieMaxAge);
    wac.setCookieName(wacCookieName);
    wac.setCookiePath(wacCookiePath);
    wac.setCookieSecure(wacCookieSecure);
    wac.setIdLength(wacIdLength);
    wac.setServerId(wacServerId);
    wac.setTimeoutSeconds(wacTimeout);
    wac.setTrackingEnabled(wacTrackingEnabled);
    wac.setUrlEnabled(wacUrlEnabled);

    Properties spc = new Properties();
    spc.setProperty(ConfigProperties.COOKIE_COMMENT, spcCookieComment);
    spc.setProperty(ConfigProperties.COOKIE_DOMAIN, spcCookieDomain);
    spc.setProperty(ConfigProperties.COOKIE_ENABLED, String.valueOf(spcCookieEnabled));
    spc.setProperty(ConfigProperties.COOKIE_MAX_AGE, String.valueOf(spcCookieMaxAge));
    spc.setProperty(ConfigProperties.COOKIE_NAME, spcCookieName);
    spc.setProperty(ConfigProperties.COOKIE_PATH, spcCookiePath);
    spc.setProperty(ConfigProperties.COOKIE_SECURE, String.valueOf(spcCookieSecure));
    spc.setProperty(ConfigProperties.ID_LENGTH, String.valueOf(spcIdLength));
    spc.setProperty(ConfigProperties.INVALIDATOR_SLEEP, String.valueOf(spcInvalidatorSec));
    spc.setProperty(ConfigProperties.SERVER_ID, spcServerId);
    spc.setProperty(ConfigProperties.SESSION_TIMEOUT_SECONDS, String.valueOf(spcTimeout));
    spc.setProperty(ConfigProperties.TRACKING_ENABLED, String.valueOf(spcTrackingEnabled));
    spc.setProperty(ConfigProperties.TRACKING_ENABLED, String.valueOf(spcUrlEnabled));
    spc.setProperty(ConfigProperties.URL_REWRITE_ENABLED, String.valueOf(spcUrlEnabled));

    tpc = new MockTCPropeties(spc);

  }

  public void testConfigProperties() {
    try {
      new ConfigProperties(null, TCPropertiesImpl.getProperties());
      new ConfigProperties(new MockWebAppConfig(), TCPropertiesImpl.getProperties());
    } catch (Exception e) {
      fail("unexpected exception");
    }
  }

  public void testSystemPropertyOverrides() {
    ConfigProperties cp = new ConfigProperties(wac, tpc);
    assertEquals(spcCookieEnabled, cp.getCookiesEnabled());
    assertEquals(spcTrackingEnabled, cp.getSessionTrackingEnabled());
    assertEquals(spcUrlEnabled, cp.getUrlRewritingEnabled());
    assertEquals(spcCookieComment, cp.getCookieCoomment());
    assertEquals(spcCookieDomain, cp.getCookieDomain());
    assertEquals(spcCookieMaxAge, cp.getCookieMaxAgeSeconds());
    assertEquals(spcInvalidatorSec, cp.getInvalidatorSleepSeconds());
    assertEquals(spcCookieName, cp.getCookieName());
    assertEquals(spcCookiePath, cp.getCookiePath());
    assertEquals(spcCookieSecure, cp.getCookieSecure());
    assertEquals(spcIdLength, cp.getSessionIdLength());
    assertEquals(spcServerId, cp.getServerId());
    assertEquals(spcTimeout, cp.getSessionTimeoutSeconds());
  }

  public void testEmptyProperties() {
    ConfigProperties cp = new ConfigProperties(wac, TCPropertiesImpl.getProperties());
    assertEquals(wacCookieEnabled, cp.getCookiesEnabled());
    assertEquals(wacTrackingEnabled, cp.getSessionTrackingEnabled());
    assertEquals(wacUrlEnabled, cp.getUrlRewritingEnabled());
    assertEquals(wacCookieComment, cp.getCookieCoomment());
    assertEquals(wacCookieDomain, cp.getCookieDomain());
    assertEquals(wacCookieMaxAge, cp.getCookieMaxAgeSeconds());
    assertEquals(wacInvalidatorSec, cp.getInvalidatorSleepSeconds());
    assertEquals(wacCookieName, cp.getCookieName());
    assertEquals(wacCookiePath, cp.getCookiePath());
    assertEquals(wacCookieSecure, cp.getCookieSecure());
    assertEquals(wacIdLength, cp.getSessionIdLength());
    assertEquals(wacServerId, cp.getServerId());
    assertEquals(wacTimeout, cp.getSessionTimeoutSeconds());
  }

  public void testDefaults() {
    ConfigProperties cp = new ConfigProperties(null, TCPropertiesImpl.getProperties());
    assertEquals(ConfigProperties.defaultCookiesEnabled, cp.getCookiesEnabled());
    assertEquals(ConfigProperties.defaultTrackingEnabled, cp.getSessionTrackingEnabled());
    assertEquals(ConfigProperties.defaultUrlEnabled, cp.getUrlRewritingEnabled());
    assertEquals(ConfigProperties.defaultCookieComment, cp.getCookieCoomment());
    assertEquals(ConfigProperties.defaultCookieDomain, cp.getCookieDomain());
    assertEquals(ConfigProperties.defaultCookieMaxAge, cp.getCookieMaxAgeSeconds());
    assertEquals(ConfigProperties.defaultCookieName, cp.getCookieName());
    assertEquals(ConfigProperties.defaultCookiePath, cp.getCookiePath());
    assertEquals(ConfigProperties.defaultCookieSecure, cp.getCookieSecure());
    assertEquals(ConfigProperties.defaultIdLength, cp.getSessionIdLength());
    assertEquals(ConfigProperties.defaultServerId, cp.getServerId());
    assertEquals(ConfigProperties.defaultSessionTimeout, cp.getSessionTimeoutSeconds());
  }

  public void testGetSessionListeners() {
    final String propVal = SessionListener.class.getName() + "," + AttributeListener.class.getName();
    final Properties props = new Properties();
    props.setProperty(ConfigProperties.SESSION_LISTENERS, propVal);
    final ConfigProperties cp = new ConfigProperties(null, new MockTCPropeties(props));
    HttpSessionListener[] listeners = cp.getSessionListeners();
    assertNotNull(listeners);
    assertEquals(1, listeners.length);
    assertTrue(listeners[0] instanceof SessionListener);
  }

  public void testGetAttibuteListeners() {
    final String propVal = SessionListener.class.getName() + "," + AttributeListener.class.getName();
    final Properties props = new Properties();
    props.setProperty(ConfigProperties.ATTRIBUTE_LISTENERS, propVal);
    final ConfigProperties cp = new ConfigProperties(null, new MockTCPropeties(props));
    HttpSessionAttributeListener[] listeners = cp.getSessionAttributeListeners();
    assertNotNull(listeners);
    assertEquals(1, listeners.length);
    assertTrue(listeners[0] instanceof AttributeListener);
  }

  public void testMakeSessionListener() {
    HttpSessionListener listener = ConfigProperties.makeSessionListener("com.some.Bogus.Type");
    assertNull(listener);

    listener = ConfigProperties.makeSessionListener(ConfigPropertiesTest.SessionListener.class.getName());
    assertTrue(listener instanceof ConfigPropertiesTest.SessionListener);

    listener = ConfigProperties.makeSessionListener(ConfigPropertiesTest.AttributeListener.class.getName());
    assertNull(listener);
  }

  public void testMakeAttributeListener() {
    HttpSessionAttributeListener listener = ConfigProperties.makeAttributeListener("com.some.Bogus.Type");
    assertNull(listener);

    listener = ConfigProperties.makeAttributeListener(ConfigPropertiesTest.AttributeListener.class.getName());
    assertTrue(listener instanceof ConfigPropertiesTest.AttributeListener);

    listener = ConfigProperties.makeAttributeListener(ConfigPropertiesTest.SessionListener.class.getName());
    assertNull(listener);
  }

  static class AttributeListener implements HttpSessionAttributeListener {

    public void attributeAdded(HttpSessionBindingEvent arg0) {
      // n/a
    }

    public void attributeRemoved(HttpSessionBindingEvent arg0) {
      // n/a
    }

    public void attributeReplaced(HttpSessionBindingEvent arg0) {
      // n/a
    }
  }

  static class SessionListener implements HttpSessionListener {

    public void sessionCreated(HttpSessionEvent arg0) {
      // n/a
    }

    public void sessionDestroyed(HttpSessionEvent arg0) {
      // n/a
    }

  }

  private static class MockTCPropeties implements TCProperties {

    private final Properties props;

    MockTCPropeties(Properties props) {
      this.props = props;
    }

    public boolean getBoolean(String key) {
      return Boolean.valueOf(getProperty(key)).booleanValue();
    }

    public int getInt(String key) {
      return Integer.valueOf(getProperty(key)).intValue();
    }

    public long getLong(String key) {
      return Long.valueOf(getProperty(key)).longValue();
    }

    public TCProperties getPropertiesFor(String key) {
      throw new ImplementMe();
    }

    public String getProperty(String key) {
      return getProperty(key, false);
    }

    public String getProperty(String key, boolean missingOkay) {
      String s = props.getProperty(key);
      if (s == null && !missingOkay) { throw new RuntimeException("missing value for " + key); }
      return s;
    }

    public float getFloat(String key) {
      return Float.valueOf(getProperty(key)).floatValue();
    }

    public Properties addAllPropertiesTo(Properties properties) {
      properties.putAll(props);
      return properties;
    }
  }

}
