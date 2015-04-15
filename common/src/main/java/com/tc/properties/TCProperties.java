/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
