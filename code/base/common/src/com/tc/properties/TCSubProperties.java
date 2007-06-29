/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.properties;

import java.util.Properties;

class TCSubProperties implements TCProperties {

  private final String           category;
  private final TCPropertiesImpl properties;

  public TCSubProperties(TCPropertiesImpl properties, String category) {
    this.properties = properties;
    this.category = category;
  }

  public TCProperties getPropertiesFor(String category2) {
    return properties.getPropertiesFor(getActualKey(category2));
  }

  private String getActualKey(String key) {
    return category + "." + key;
  }

  // TODO::REmovwe
  protected boolean containsKey(String key) {
    return key.startsWith(category + ".");
  }

  public String getProperty(String key) {
    return properties.getProperty(getActualKey(key));
  }

  public String toString() {
    return "TCSubProperties(" + category + ")";
  }

  public Properties addAllPropertiesTo(Properties dest) {
    return properties.addAllPropertiesTo(dest, category + ".");
  }

  public boolean getBoolean(String key) {
    return properties.getBoolean(getActualKey(key));
  }

  public float getFloat(String key) {
    return properties.getFloat(getActualKey(key));
  }

  public int getInt(String key) {
    return properties.getInt(getActualKey(key));
  }

  public long getLong(String key) {
    return properties.getLong(getActualKey(key));
  }

  public String getProperty(String key, boolean missingOkay) {
    return properties.getProperty(getActualKey(key), missingOkay);
  }

  public int getInt(String key, int defaultValue) {
    return properties.getInt(getActualKey(key), defaultValue);
  }
}
