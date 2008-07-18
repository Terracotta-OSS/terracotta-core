/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.config;

import org.terracotta.modules.tool.util.PropertiesInterpolator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Global configuration for the TIM Update Center application.
 */
public class Config {
  public static final String KEYSPACE = "org.terracotta.modules.tool.";

  private String tcVersion;
  private URL    proxyUrl;
  private File   modulesDirectory;
  private URL    dataFileUrl;
  private File   dataFile;

  public Config() {
    // nothing to do
  }

  public Config(Properties properties) {
    properties = new PropertiesInterpolator().interpolated(properties);
    this.setTcVersion(getProperty(properties, "tcVersion"));
    this.setDataFile(new File(getProperty(properties, "dataFile")));
    this.setDataFileUrl(createUrl(getProperty(properties, "dataFileUrl"),
                          "dataFileUrl is not a valid URL"));
    this.setModulesDirectory(new File(getProperty(properties, "modulesDir")));
    try {
      this.setDataFileUrl(new URL(getProperty(properties, "dataFileUrl")));
    } catch (MalformedURLException e) {
      throw new InvalidConfigurationException("dataFileUrl is not a valid URL", e);
    }

    String proxy = getProperty(properties, "proxyUrl");
    if (proxy != null)
      this.setProxyUrl(createUrl(proxy, "Proxy URL is not a valid URL"));
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
  public File getDataFile() {
    return dataFile;
  }
  public void setDataFile(File dataFileDirectory) {
    this.dataFile = dataFileDirectory;
  }
}
