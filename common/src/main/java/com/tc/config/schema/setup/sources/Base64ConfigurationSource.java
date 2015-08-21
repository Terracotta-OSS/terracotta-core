/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup.sources;

import org.terracotta.license.util.Base64;

import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class Base64ConfigurationSource implements ConfigurationSource {
  private final String configString;

  public Base64ConfigurationSource(String base64ConfigString) {
    Assert.assertNotBlank(base64ConfigString);
    try {
      this.configString = new String(Base64.decode(base64ConfigString), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream getInputStream(long maxTimeoutMillis) {
    try {
      return new ByteArrayInputStream(configString.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
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
    return "string";
  }
}
