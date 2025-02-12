/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.properties;

import java.util.Map;
import java.util.Properties;

class TCSubProperties implements TCProperties {

  private final String           category;
  private final TCPropertiesImpl properties;

  public TCSubProperties(TCPropertiesImpl properties, String category) {
    this.properties = properties;
    this.category = category;
  }

  @Override
  public TCProperties getPropertiesFor(String category2) {
    return properties.getPropertiesFor(getActualKey(category2));
  }

  private String getActualKey(String key) {
    return category + "." + key;
  }

  @Override
  public String getProperty(String key) {
    return properties.getProperty(getActualKey(key));
  }

  @Override
  public String toString() {
    return "TCSubProperties(" + category + ")";
  }

  @Override
  public Properties addAllPropertiesTo(Properties dest) {
    return properties.addAllPropertiesTo(dest, category + ".");
  }

  @Override
  public boolean getBoolean(String key) {
    return properties.getBoolean(getActualKey(key));
  }

  @Override
  public float getFloat(String key) {
    return properties.getFloat(getActualKey(key));
  }

  @Override
  public int getInt(String key) {
    return properties.getInt(getActualKey(key));
  }

  @Override
  public long getLong(String key) {
    return properties.getLong(getActualKey(key));
  }

  @Override
  public long getLong(String key, long defaultValue) {
    return properties.getLong(getActualKey(key), defaultValue);
  }

  @Override
  public String getProperty(String key, boolean missingOkay) {
    return properties.getProperty(getActualKey(key), missingOkay);
  }

  @Override
  public int getInt(String key, int defaultValue) {
    return properties.getInt(getActualKey(key), defaultValue);
  }

  @Override
  public boolean getBoolean(String key, boolean defaultValue) {
    return properties.getBoolean(getActualKey(key), defaultValue);
  }

  @Override
  public void overwriteTcPropertiesFromConfig(Map<String, String> props) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setProperty(String key, String value) {
    properties.setProperty(getActualKey(key), value);
  }

}
