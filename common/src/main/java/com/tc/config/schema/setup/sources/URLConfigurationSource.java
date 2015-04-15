/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
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

  @Override
  public File directoryLoadedFrom() {
    return null;
  }

  @Override
  public boolean isTrusted() {
    return false;
  }

  @Override
  public String toString() {
    return "URL '" + this.url + "'";
  }

}
