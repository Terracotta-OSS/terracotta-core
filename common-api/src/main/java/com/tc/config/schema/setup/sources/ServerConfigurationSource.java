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
public class ServerConfigurationSource implements ConfigurationSource {

  private final String host;
  private final int port;

  public ServerConfigurationSource(String host, int port) {
    Assert.assertNotBlank(host);
    Assert.assertTrue(port > 0);
    this.host = host;
    this.port = port;
  }

  public InputStream getInputStream(long maxTimeoutMillis) throws IOException, ConfigurationSetupException {
    try {
      URL theURL = new URL("http", host, port, "/config");
  
      // JDK: 1.4.2 - These settings are proprietary to Sun's implementation of java.net.URL in version 1.4.2
      System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(maxTimeoutMillis));
      System.setProperty("sun.net.client.defaultReadTimeout", String.valueOf(maxTimeoutMillis));

      URLConnection connection = theURL.openConnection();
      return connection.getInputStream();
    } catch (MalformedURLException murle) {
      throw new ConfigurationSetupException("Can't load configuration from "+this+".");
    }
  }

  public File directoryLoadedFrom() {
    return null;
  }

  public boolean isTrusted() {
    return true;
  }

  public String toString() {
    return "server at '" + this.host + ":" + this.port + "'";
  }

}
