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
