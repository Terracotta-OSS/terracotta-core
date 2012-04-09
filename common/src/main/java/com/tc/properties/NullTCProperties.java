/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.properties;

import java.util.Map;
import java.util.Properties;

public class NullTCProperties implements TCProperties {

  public static final TCProperties INSTANCE = new NullTCProperties();

  private NullTCProperties() {
    //
  }

  @Override
  public int getInt(String key) {
    //
    return 0;
  }

  @Override
  public int getInt(String key, int defaultValue) {
    //
    return 0;
  }

  @Override
  public long getLong(String key) {
    //
    return 0;
  }

  @Override
  public long getLong(String key, long defaultValue) {
    //
    return 0;
  }

  @Override
  public boolean getBoolean(String key) {
    //
    return false;
  }

  @Override
  public boolean getBoolean(String key, boolean defaultValue) {
    //
    return false;
  }

  @Override
  public String getProperty(String key) {
    //
    return null;
  }

  @Override
  public TCProperties getPropertiesFor(String key) {
    //
    return null;
  }

  @Override
  public String getProperty(String key, boolean missingOkay) {
    //
    return null;
  }

  @Override
  public float getFloat(String key) {
    //
    return 0;
  }

  @Override
  public Properties addAllPropertiesTo(Properties properties) {
    //
    return null;
  }

  @Override
  public void overwriteTcPropertiesFromConfig(Map<String, String> props) {
    //

  }

  @Override
  public void setProperty(String key, String value) {
    //

  }

}
