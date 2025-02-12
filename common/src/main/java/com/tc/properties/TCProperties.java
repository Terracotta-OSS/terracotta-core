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

  void overwriteTcPropertiesFromConfig(Map<String, String> props);

  void setProperty(String key, String value);
}
