/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.config;

import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.tool.util.PropertiesInterpolator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * Global configuration for the TIM Update Center application.
 */
public class Config {

  public static final String KEYSPACE          = "org.terracotta.modules.tool.";

  public static final String TC_VERSION        = "tcVersion";
  public static final String RELATIVE_URL_BASE = "relativeUrlBase";
  public static final String INCLUDE_SNAPSHOTS = "includeSnapshots";
  public static final String PROXY_URL         = "proxyUrl";
  public static final String MODULES_DIR       = "modulesDir";
  public static final String DATA_FILE_URL     = "dataFileUrl";
  public static final String CACHE             = "cache";

  private String             tcVersion;
  private URI                relativeUrlBase;
  private boolean            includeSnapshots;
  private URL                proxyUrl;
  private File               modulesDirectory;
  private URL                dataFileUrl;
  private File               indexFile;
  private long               dataCacheExpirationInSeconds;

  // We need to declare this no arg contructor so it can be Guice'd
  Config() {
    // nothing to do
  }

  public Config(Properties properties) {
    properties = new PropertiesInterpolator().interpolated(properties);
    this.setTcVersion(getProperty(properties, TC_VERSION));

    try {
      this.setRelativeUrlBase(new URI(getProperty(properties, RELATIVE_URL_BASE)));
    } catch (URISyntaxException e) {
      throw new InvalidConfigurationException(RELATIVE_URL_BASE + " is not a valid URL");
    }
    this.setIncludeSnapshots(Boolean.parseBoolean(getProperty(properties, INCLUDE_SNAPSHOTS)));

    String dataUrl = getProperty(properties, DATA_FILE_URL);
    this.setDataFileUrl(createUrl(dataUrl, "dataFileUrl is not a valid URL"));
    this.setModulesDirectory(new File(getProperty(properties, MODULES_DIR)));
    this.setDataCacheExpirationInSeconds(Long.parseLong(getProperty(properties, "dataCacheExpirationInSeconds")));

    String cachePath = getProperty(properties, CACHE);
    if (StringUtils.isEmpty(cachePath)) cachePath = System.getProperty("java.io.tmpdir");
    cachePath = cachePath + File.separator + "tim-get";
    this.setIndexFile(new File(cachePath, new File(dataUrl).getName()));

    String proxy = getProperty(properties, PROXY_URL);
    if (proxy != null) this.setProxyUrl(createUrl(proxy, "Proxy URL is not a valid URL"));
  }

  private static URL createUrl(String urlString, String errorMessage) {
    try {
      return new URL(urlString);
    } catch (MalformedURLException e) {
      throw new InvalidConfigurationException(errorMessage, e);
    }
  }

  private static String getProperty(Properties props, String name) {
    return getProperty(props, name, null);
  }

  private static String getProperty(Properties props, String name, String defaultValue) {
    return System.getProperty(KEYSPACE + name, props.getProperty(KEYSPACE + name, defaultValue));
  }

  public URL getProxyUrl() {
    return proxyUrl;
  }

  public void setProxyUrl(URL proxyUrl) {
    this.proxyUrl = proxyUrl;
  }

  public String getTcVersion() {
    return tcVersion;
  }

  public void setTcVersion(String tcVersion) {
    this.tcVersion = tcVersion;
  }

  public URI getRelativeUrlBase() {
    return relativeUrlBase;
  }

  public void setRelativeUrlBase(URI relativeUrlBase) {
    this.relativeUrlBase = relativeUrlBase;
  }

  public File getModulesDirectory() {
    return modulesDirectory;
  }

  public void setModulesDirectory(File modulesDirectory) {
    this.modulesDirectory = modulesDirectory;
  }

  public URL getDataFileUrl() {
    return dataFileUrl;
  }

  public void setDataFileUrl(URL dataFileUrl) {
    this.dataFileUrl = dataFileUrl;
  }

  public File getIndexFile() {
    return indexFile;
  }

  public void setIndexFile(File file) {
    this.indexFile = file;
  }

  public long getDataCacheExpirationInSeconds() {
    return dataCacheExpirationInSeconds;
  }

  public void setDataCacheExpirationInSeconds(long dataCacheExpirationInSeconds) {
    this.dataCacheExpirationInSeconds = dataCacheExpirationInSeconds;
  }

  public void setIncludeSnapshots(boolean includeSnapshots) {
    this.includeSnapshots = includeSnapshots;
  }

  public boolean getIncludeSnapshots() {
    return includeSnapshots;
  }
}
