/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.util.Properties;

/**
 * @author Hung Huynh
 */
public class LicenseEntity extends AbstractTsaEntity{
  private Properties properties = new Properties();

  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }
}
