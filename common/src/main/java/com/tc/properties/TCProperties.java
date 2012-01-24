/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.properties;

import java.util.Map;
import java.util.Properties;

public interface TCProperties {

  int getInt(String key);

  int getInt(String key, int defaultValue);

  long getLong(String key);

  long getLong(String key, long defaultValue);

  boolean getBoolean(String key);

  boolean getBoolean(String key, boolean defaultValue);

  String getProperty(String key);

  TCProperties getPropertiesFor(String key);

  String getProperty(String key, boolean missingOkay);

  float getFloat(String key);

  Properties addAllPropertiesTo(Properties properties);

  public void overwriteTcPropertiesFromConfig(Map<String, String> props);

  public void setProperty(String key, String value);
}
