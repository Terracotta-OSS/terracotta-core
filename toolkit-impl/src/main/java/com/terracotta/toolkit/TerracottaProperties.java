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
package com.terracotta.toolkit;

import org.terracotta.toolkit.internal.ToolkitProperties;

import com.tc.platform.PlatformService;
import com.tc.properties.TCProperties;

public class TerracottaProperties implements ToolkitProperties {
  private final TCProperties delegate;

  public TerracottaProperties(PlatformService platformService) {
    this.delegate = platformService.getTCProperties();
  }

  @Override
  public Boolean getBoolean(String key, Boolean defaultValue) {
    String str = getProperty(key);
    if (str == null) {
      return defaultValue;
    } else {
      return Boolean.valueOf(str);
    }
  }

  @Override
  public Boolean getBoolean(String key) {
    return getBoolean(key, Boolean.FALSE);
  }

  @Override
  public Integer getInteger(String key, Integer defaultValue) {
    try {
      return Integer.valueOf(getProperty(key));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  @Override
  public Integer getInteger(String key) {
    return getInteger(key, null);
  }

  @Override
  public Long getLong(String key, Long defaultValue) {
    try {
      return Long.valueOf(getProperty(key));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  @Override
  public Long getLong(String key) {
    return getLong(key, null);
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    String value = getProperty(key);
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

  @Override
  public String getProperty(String key) {
    return delegate.getProperty(key, true);
  }

  @Override
  public void setProperty(String key, String value) {
    delegate.setProperty(key, value);
  }
}
