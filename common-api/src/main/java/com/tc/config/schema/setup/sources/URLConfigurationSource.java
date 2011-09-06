/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup.sources;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * A {@link ConfigurationSource} that reads from a URL.
 * 
 * @see URLConfigurationSourceTest
 */
public class URLConfigurationSource implements ConfigurationSource {

  private final String url;

  public URLConfigurationSource(String url) {
    Assert.assertNotBlank(url);
    this.url = url;
  }

  public InputStream getInputStream(long maxTimeoutMillis) throws IOException, ConfigurationSetupException {
    URL theURL = new URL(this.url);
    
    // JDK: 1.4.2 - These settings are proprietary to Sun's implementation of java.net.URL in version 1.4.2
    System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(maxTimeoutMillis));
    System.setProperty("sun.net.client.defaultReadTimeout", String.valueOf(maxTimeoutMillis));
    
    try {
      URLConnection connection = theURL.openConnection();
      return connection.getInputStream();
    } catch (MalformedURLException murle) {
      throw new ConfigurationSetupException("The URL '" + this.url
                                            + "' is malformed, and thus can't be used to load configuration.");
    }
  }

  public File directoryLoadedFrom() {
    return null;
  }

  public boolean isTrusted() {
    return false;
  }

  public String toString() {
    return "URL '" + this.url + "'";
  }

}
