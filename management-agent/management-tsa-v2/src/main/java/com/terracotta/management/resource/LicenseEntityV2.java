/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.util.Properties;

/**
 * @author Hung Huynh
 */
public class LicenseEntityV2 extends AbstractTsaEntityV2 {
  private Properties properties = new Properties();
  private String     sourceId;

  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }
}
