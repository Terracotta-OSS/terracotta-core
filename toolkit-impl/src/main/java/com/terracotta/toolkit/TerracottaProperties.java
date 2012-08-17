/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.properties.TCProperties;

public class TerracottaProperties {
  private final TCProperties delegate;

  public TerracottaProperties() {
    this.delegate = ManagerUtil.getTCProperties();
  }

  public Boolean getBoolean(String key, Boolean defaultValue) {
    String str = getProperty(key);
    if (str == null) {
      return defaultValue;
    } else {
      return Boolean.valueOf(str);
    }
  }

  public Boolean getBoolean(String key) {
    return getBoolean(key, Boolean.FALSE);
  }

  public Integer getInteger(String key, Integer defaultValue) {
    try {
      return Integer.valueOf(getProperty(key));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public Integer getInteger(String key) {
    return getInteger(key, null);
  }

  public Long getLong(String key, Long defaultValue) {
    try {
      return Long.valueOf(getProperty(key));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public Long getLong(String key) {
    return getLong(key, null);
  }

  public String getProperty(String key, String defaultValue) {
    String value = getProperty(key);
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

  public String getProperty(String key) {
    return delegate.getProperty(key, true);
  }

  public void setProperty(String key, String value) {
    delegate.setProperty(key, value);
  }
}
