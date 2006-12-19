/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.properties;


class TCSubProperties extends TCPropertiesImpl {

  private final String category;
  private final TCProperties properties;

  public TCSubProperties(TCProperties properties, String category) {
    super(category);
    this.properties = properties;
    this.category = category;
  }

  public TCProperties getPropertiesFor(String category2) {
    return super.getPropertiesFor(getActualKey(category2));
  }

  private String getActualKey(String key) {
    return category + "." + key;
  }

  public String getProperty(String key) {
    return properties.getProperty(getActualKey(key));
  }

  public String toString() {
    return "TCSubProperties("+category+")";
  }
}
