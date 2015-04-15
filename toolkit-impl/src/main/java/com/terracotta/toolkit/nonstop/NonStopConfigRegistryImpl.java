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
package com.terracotta.toolkit.nonstop;

import static org.terracotta.toolkit.ToolkitObjectType.ATOMIC_LONG;
import static org.terracotta.toolkit.ToolkitObjectType.CACHE;
import static org.terracotta.toolkit.ToolkitObjectType.LIST;
import static org.terracotta.toolkit.ToolkitObjectType.LOCK;
import static org.terracotta.toolkit.ToolkitObjectType.MAP;
import static org.terracotta.toolkit.ToolkitObjectType.NOTIFIER;
import static org.terracotta.toolkit.ToolkitObjectType.READ_WRITE_LOCK;
import static org.terracotta.toolkit.ToolkitObjectType.SET;
import static org.terracotta.toolkit.ToolkitObjectType.SORTED_MAP;
import static org.terracotta.toolkit.ToolkitObjectType.SORTED_SET;
import static org.terracotta.toolkit.ToolkitObjectType.STORE;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopReadTimeoutBehavior;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopWriteTimeoutBehavior;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NonStopConfigRegistryImpl implements NonStopConfigurationRegistry {
  public static final EnumSet<ToolkitObjectType>                      SUPPORTED_TOOLKIT_TYPES  = EnumSet
                                                                                                   .of(STORE, CACHE,
                                                                                                       LIST, LOCK,
                                                                                                       READ_WRITE_LOCK,
                                                                                                       MAP, SORTED_MAP,
                                                                                                       SORTED_SET, SET,
                                                                                                       NOTIFIER,
                                                                                                       ATOMIC_LONG);

  public static final NonStopConfiguration                            DEFAULT_CONFIG           = new NonStopConfiguration() {

                                                                                                 @Override
                                                                                                 public long getTimeoutMillis() {
                                                                                                   return NonStopConfigurationFields.DEFAULT_TIMEOUT_MILLIS;
                                                                                                 }

                                                                                                 @Override
                                                                                                 public long getSearchTimeoutMillis() {
                                                                                                     return NonStopConfigurationFields.DEFAULT_SEARCH_TIMEOUT_MILLIS;
                                                                                                 }

                                                                                                 @Override
                                                                                                 public boolean isEnabled() {
                                                                                                   return NonStopConfigurationFields.DEFAULT_NON_STOP_ENABLED;
                                                                                                 }

                                                                                                 @Override
                                                                                                 public boolean isImmediateTimeoutEnabled() {
                                                                                                   return NonStopConfigurationFields.DEFAULT_NON_STOP_IMMEDIATE_TIMEOUT_ENABLED;
                                                                                                 }

                                                                                                 @Override
                                                                                                 public NonStopConfigurationFields.NonStopReadTimeoutBehavior getReadOpNonStopTimeoutBehavior() {
                                                                                                   return NonStopConfigurationFields.DEFAULT_NON_STOP_READ_TIMEOUT_BEHAVIOR;
                                                                                                 }

                                                                                                 @Override
                                                                                                 public NonStopConfigurationFields.NonStopWriteTimeoutBehavior getWriteOpNonStopTimeoutBehavior() {
                                                                                                   return NonStopConfigurationFields.DEFAULT_NON_STOP_WRITE_TIMEOUT_BEHAVIOR;
                                                                                                 }
                                                                                               };

  private final ThreadLocal<NonStopConfiguration>                     threadLocalConfiguration = new ThreadLocal<NonStopConfiguration>();
  private final ConcurrentMap<NonStopConfigKey, NonStopConfiguration> allConfigs               = new ConcurrentHashMap<NonStopConfigKey, NonStopConfiguration>();
  private final ConcurrentMap<String, Long>                           searchTimeoutsCache      = new ConcurrentHashMap<String, Long>();
  private final ConcurrentMap<String, Long>                           searchTimeoutsStore      = new ConcurrentHashMap<String, Long>();

  private void verify(NonStopConfiguration nonStopConfiguration, ToolkitObjectType... types) {
    if (types != null) {
      for (ToolkitObjectType nonStopToolkitTypeParam : types) {
        if (!SUPPORTED_TOOLKIT_TYPES.contains(nonStopToolkitTypeParam)) { throw new UnsupportedOperationException(
                                                                                                                  nonStopToolkitTypeParam
                                                                                                                      .name()
                                                                                                                      + " is not yet supported as a non stop data structure"); }
        if (nonStopToolkitTypeParam != CACHE && nonStopToolkitTypeParam != STORE) {
          if (nonStopConfiguration.getWriteOpNonStopTimeoutBehavior() != NonStopWriteTimeoutBehavior.EXCEPTION) { throw new UnsupportedOperationException(
                                                                                                                                                          "behavior "
                                                                                                                                                              + nonStopConfiguration
                                                                                                                                                                  .getWriteOpNonStopTimeoutBehavior()
                                                                                                                                                              + " not supported for "
                                                                                                                                                              + nonStopToolkitTypeParam); }

          if (nonStopConfiguration.getReadOpNonStopTimeoutBehavior() != NonStopReadTimeoutBehavior.EXCEPTION) { throw new UnsupportedOperationException(
                                                                                                                                                        "behavior "
                                                                                                                                                            + nonStopConfiguration
                                                                                                                                                                .getReadOpNonStopTimeoutBehavior()
                                                                                                                                                            + " not supported for "
                                                                                                                                                            + nonStopToolkitTypeParam); }
        }
      }

    }
  }

  @Override
  public void registerForType(NonStopConfiguration config, ToolkitObjectType... types) {
    verify(config, types);

    for (ToolkitObjectType type : types) {
      allConfigs.put(new NonStopConfigKey(null, type, null), config);
    }
  }

  @Override
  public void registerForInstance(NonStopConfiguration config, String toolkitTypeName, ToolkitObjectType type) {
    verify(config, type);

    allConfigs.put(new NonStopConfigKey(null, type, toolkitTypeName), config);
  }

  @Override
  public void registerForTypeMethod(NonStopConfiguration config, String methodName, ToolkitObjectType type) {
    verify(config, type);

    allConfigs.put(new NonStopConfigKey(methodName, type, null), config);
  }

  @Override
  public void registerForInstanceMethod(NonStopConfiguration config, String methodName, String toolkitTypeName,
                                        ToolkitObjectType type) {
    verify(config, type);

    allConfigs.put(new NonStopConfigKey(methodName, type, toolkitTypeName), config);
  }

  public void registerForThread(NonStopConfiguration config) {

    if (config.getWriteOpNonStopTimeoutBehavior() != NonStopWriteTimeoutBehavior.EXCEPTION) { throw new UnsupportedOperationException(
                                                                                                                                      "unsupported write behavior: "
                                                                                                                                          + config
                                                                                                                                              .getWriteOpNonStopTimeoutBehavior()); }
    if (config.getReadOpNonStopTimeoutBehavior() != NonStopReadTimeoutBehavior.EXCEPTION) { throw new UnsupportedOperationException(
                                                                                                                                    "unsupported read behavior: "
                                                                                                                                        + config
                                                                                                                                            .getReadOpNonStopTimeoutBehavior()); }
    threadLocalConfiguration.set(config);
  }

  @Override
  public void registerTimeoutForSearch(long timeout, String instanceName, ToolkitObjectType objectType) {
    if (timeout <= 0) { throw new IllegalArgumentException("Timeout cannot be less than 0: " + timeout); }

    if (objectType == ToolkitObjectType.CACHE) {
      searchTimeoutsCache.put(instanceName, timeout);
    } else if (objectType == ToolkitObjectType.STORE) {
      searchTimeoutsStore.put(instanceName, timeout);
    } else {
      throw new UnsupportedOperationException("Not supported for type: " + objectType);
    }
  }

  @Override
  public NonStopConfiguration getConfigForType(ToolkitObjectType type) {
    NonStopConfiguration nonStopConfig = getConfigForThread();
    if (nonStopConfig != null) { return nonStopConfig; }

    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, null);
    nonStopConfig = allConfigs.get(nonStopConfigKey);
    if (nonStopConfig != null) {
      return nonStopConfig;
    } else {
      return DEFAULT_CONFIG;
    }
  }

  @Override
  public NonStopConfiguration getConfigForInstance(String toolkitTypeName, ToolkitObjectType type) {
    NonStopConfiguration nonStopConfig = getConfigForThread();
    if (nonStopConfig != null) { return nonStopConfig; }

    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, toolkitTypeName);
    nonStopConfig = allConfigs.get(nonStopConfigKey);
    if (nonStopConfig == null) {
      nonStopConfig = getConfigForType(type);
    }
    return nonStopConfig;
  }

  @Override
  public NonStopConfiguration getConfigForTypeMethod(String methodName, ToolkitObjectType type) {
    NonStopConfiguration nonStopConfig = getConfigForThread();
    if (nonStopConfig != null) { return nonStopConfig; }

    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, null);
    nonStopConfig = allConfigs.get(nonStopConfigKey);
    if (nonStopConfig == null) {
      nonStopConfig = getConfigForType(type);
    }
    return nonStopConfig;
  }

  @Override
  public NonStopConfiguration getConfigForInstanceMethod(String methodName, String toolkitTypeName,
                                                         ToolkitObjectType type) {
    NonStopConfiguration nonStopConfig = getConfigForThread();
    if (nonStopConfig != null) { return nonStopConfig; }

    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, toolkitTypeName);
    nonStopConfig = allConfigs.get(nonStopConfigKey);

    // try for instance
    if (nonStopConfig == null) {
      nonStopConfigKey = new NonStopConfigKey(null, type, toolkitTypeName);
      nonStopConfig = allConfigs.get(nonStopConfigKey);
    }

    // try method & type
    if (nonStopConfig == null) {
      nonStopConfigKey = new NonStopConfigKey(methodName, type, null);
      nonStopConfig = allConfigs.get(nonStopConfigKey);
    }
    // try config for type
    if (nonStopConfig == null) {
      nonStopConfig = getConfigForType(type);
    }

    return nonStopConfig;
  }

  public NonStopConfiguration getConfigForThread() {
    return threadLocalConfiguration.get();
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public long getTimeoutForSearch(String instanceName, ToolkitObjectType objectType) {
    Long searchTimeout;
    if (objectType == ToolkitObjectType.CACHE) {
      searchTimeout = searchTimeoutsCache.get(instanceName);
    } else {
      searchTimeout = searchTimeoutsStore.get(instanceName);
    }
    if (searchTimeout == null) {
      searchTimeout = getConfigForInstance(instanceName, objectType).getSearchTimeoutMillis();
    }
    return searchTimeout;
  }

  @Override
  public NonStopConfiguration deregisterForType(ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, null);
    return allConfigs.remove(nonStopConfigKey);
  }

  @Override
  public NonStopConfiguration deregisterForInstance(String toolkitTypeName, ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, toolkitTypeName);
    return allConfigs.remove(nonStopConfigKey);
  }

  @Override
  public NonStopConfiguration deregisterForTypeMethod(String methodName, ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, null);
    return allConfigs.remove(nonStopConfigKey);
  }

  @Override
  public NonStopConfiguration deregisterForInstanceMethod(String methodName, String toolkitTypeName,
                                                          ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, toolkitTypeName);
    return allConfigs.remove(nonStopConfigKey);
  }

  public NonStopConfiguration deregisterForThread() {
    NonStopConfiguration old = threadLocalConfiguration.get();
    threadLocalConfiguration.remove();

    return old;
  }

  @Override
  public long deregisterTimeoutForSearch(String instanceName, ToolkitObjectType objectType) {
    if (objectType == ToolkitObjectType.CACHE) {
      return searchTimeoutsCache.remove(instanceName);
    } else {
      return searchTimeoutsStore.remove(instanceName);
    }
  }

  private static class NonStopConfigKey {
    private final String            method;
    private final String            name;
    private final ToolkitObjectType objectType;

    public NonStopConfigKey(String method, ToolkitObjectType objectType, String name) {
      this.method = method;
      this.name = name;
      this.objectType = objectType;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((method == null) ? 0 : method.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      NonStopConfigKey other = (NonStopConfigKey) obj;
      if (method == null) {
        if (other.method != null) return false;
      } else if (!method.equals(other.method)) return false;
      if (name == null) {
        if (other.name != null) return false;
      } else if (!name.equals(other.name)) return false;
      if (objectType != other.objectType) return false;
      return true;
    }

  }
}
