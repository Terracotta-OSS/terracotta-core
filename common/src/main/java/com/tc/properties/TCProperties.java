/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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

public interface TCProperties {

  default int getInt(String key) {
    return Integer.parseInt(getProperty(key));
  }

  default int getInt(String key, int defaultValue) {
    String val = getProperty(key, true);
    if (val == null) {
      return defaultValue;
    } else {
      return Integer.parseInt(val);
    }
  }

  default long getLong(String key) {
    return Long.parseLong(getProperty(key));
  }

  default long getLong(String key, long defaultValue) {
    String val = getProperty(key, true);
    if (val == null) {
      return defaultValue;
    } else {
      return Long.parseLong(val);
    }
  }

  default boolean getBoolean(String key) {
    return Boolean.parseBoolean(getProperty(key));
  }

  default boolean getBoolean(String key, boolean defaultValue) {
    String val = getProperty(key, true);
    if (val == null) {
      return defaultValue;
    } else {
      return Boolean.parseBoolean(val);
    }
  }

  default String getProperty(String key) {
    return getProperty(key, false);
  }

  TCProperties getPropertiesFor(String key);

  String getProperty(String key, boolean missingOkay);

  default float getFloat(String key) {
    return Float.parseFloat(getProperty(key));
  }

  Properties addAllPropertiesTo(Properties properties);

  void overwriteTcPropertiesFromConfig(Map<String, String> props);

  void setProperty(String key, String value);
}
