/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.terracotta.license.License;
import org.terracotta.license.LicenseException;

import com.tc.license.LicenseManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.ProductInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

class UpdateCheckAction extends TimerTask {
  private static final TCLogger     LOG                   = TCLogging.getLogger(UpdateCheckAction.class);

  private static final int          CONNECT_TIMEOUT       = 3000;
  private static final long         MILLIS_PER_SECOND     = 1000L;
  private static final String       UNKNOWN               = "UNKNOWN";
  private static String             UPDATE_PROPERTIES_URL = "http://www.terracotta.org/kit/reflector?kitID=default&pageID=update.properties";
  private static final long         START_TIME            = System.currentTimeMillis();

  private static ProductInfo        productInfo           = ProductInfo.getInstance();

  private final long                periodMillis;
  private final long                maxOffheap;
  private License                   license;
  private final Map<String, String> params                = new HashMap<String, String>();

  UpdateCheckAction(int periodDays, long maxOffheap) {
    super();
    try {
      license = LicenseManager.getLicense();
    } catch (LicenseException e) {
      // ignore, no license given
    }
    periodMillis = checkPeriodMillis(periodDays);
    this.maxOffheap = maxOffheap;
    silenceHttpClientLoggers();
    prepareParams();
  }

  private void prepareParams() {
    putUrlSafe("id", Integer.toString(getClientId()));
    putUrlSafe("os-name", getProperty("os.name"));
    putUrlSafe("jvm-name", getProperty("java.vm.name"));
    putUrlSafe("jvm-version", getProperty("java.version"));
    putUrlSafe("platform", getProperty("os.arch"));
    putUrlSafe("tc-version", productInfo.version());
    putUrlSafe("uptime-secs", Long.toString(getUptimeInSeconds()));
    putUrlSafe("patch", productInfo.patchLevel());
    putUrlSafe("storage-size", Long.toString(maxOffheap));
    putUrlSafe("source", "BigMemory L2");
    if (license != null) {
      putUrlSafe("tc-product", license.product() + " " + productInfo.version());
      putUrlSafe("max-client-count", Integer.toString(license.maxClientCount()));
      putUrlSafe("type", license.type());
      putUrlSafe("number", license.number());
    } else {
      putUrlSafe("tc-product", "BigMemory Max " + productInfo.version());
    }
  }

  private long getUptimeInSeconds() {
    long uptime = System.currentTimeMillis() - START_TIME;
    return uptime > 0 ? (uptime / MILLIS_PER_SECOND) : 0;
  }

  protected void putUrlSafe(String key, String value) {
    params.put(key, urlEncode(value));
  }

  private int getClientId() {
    try {
      return InetAddress.getLocalHost().hashCode();
    } catch (Throwable t) {
      return 0;
    }
  }

  private String urlEncode(String param) {
    try {
      return URLEncoder.encode(param, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }

  }

  private String getProperty(String prop) {
    return System.getProperty(prop, UNKNOWN);
  }

  /**
   * hook point to update any params before update check
   */
  protected void updateParams() {
    putUrlSafe("uptime-secs", Long.toString(getUptimeInSeconds()));
  }

  public static void start(int periodDays, long maxOffheap) {
    UpdateCheckAction action = new UpdateCheckAction(periodDays, maxOffheap);
    new Timer("Update Checker", true).schedule(action, 0, action.getCheckPeriodMillis());
  }

  public long getCheckPeriodMillis() {
    return periodMillis;
  }

  private static void silenceHttpClientLoggers() {
    Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
    Logger.getLogger("httpclient.wire").setLevel(Level.OFF);
  }

  private String buildParamsString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : params.entrySet()) {
      sb.append("&").append(entry.getKey()).append("=").append(entry.getValue());
    }
    if (sb.length() > 1) {
      sb.deleteCharAt(0);
    }
    return sb.toString();
  }

  private URL buildUpdateCheckUrl() throws MalformedURLException {
    String url = System.getProperty("terracotta.update-checker.url", UPDATE_PROPERTIES_URL);
    String connector = url.indexOf('?') > 0 ? "&" : "?";
    return new URL(url + connector + buildParamsString());
  }

  private Properties getUpdateProperties(URL updateUrl) throws IOException {
    URLConnection connection = updateUrl.openConnection();
    connection.setConnectTimeout(CONNECT_TIMEOUT);
    InputStream in = connection.getInputStream();
    try {
      Properties props = new Properties();
      props.load(connection.getInputStream());
      return props;
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

  private boolean notBlank(String s) {
    return s != null && s.trim().length() > 0;
  }

  private void doUpdateCheck() throws ConnectException, IOException {
    updateParams();
    URL updateUrl = buildUpdateCheckUrl();
    if (Boolean.getBoolean("com.tc.debug.updatecheck")) {
      LOG.info("Update check url " + updateUrl);
    }
    Properties updateProps = getUpdateProperties(updateUrl);
    String currentVersion = productInfo.version();
    String propVal = updateProps.getProperty("general.notice");
    if (notBlank(propVal)) {
      LOG.info(propVal);
    }
    propVal = updateProps.getProperty(currentVersion + ".notices");
    if (notBlank(propVal)) {
      LOG.info(propVal);
    }
    propVal = updateProps.getProperty(currentVersion + ".updates");
    if (notBlank(propVal)) {
      StringBuilder sb = new StringBuilder();
      String[] newVersions = propVal.split(",");
      for (int i = 0; i < newVersions.length; i++) {
        String newVersion = newVersions[i].trim();
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(newVersion);
        propVal = updateProps.getProperty(newVersion + ".release-notes");
        if (notBlank(propVal)) {
          sb.append(" [");
          sb.append(propVal);
          sb.append("]");
        }
      }
      if (sb.length() > 0) {
        LOG.info("New update(s) found: " + sb.toString());
      }
    }
  }

  @Override
  public void run() {
    try {
      doUpdateCheck();
    } catch (Throwable t) {
      LOG.debug("Update check failed: ", t);
    }
  }

  private static long checkPeriodMillis(int days) {
    Long minutes = Long.getLong("terracotta.update-checker.next-check-minutes");
    long nextCheckTime;

    if (minutes != null) {
      nextCheckTime = minutes.longValue() * 60 * 1000;
    } else {
      nextCheckTime = 1000L * 60 * 60 * 24 * days;
    }

    return nextCheckTime;
  }
}
