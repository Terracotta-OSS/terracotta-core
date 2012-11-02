/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.nonstop.NonStopConfig;
import org.terracotta.toolkit.nonstop.NonStopConfigRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NonStopConfigRegistryImpl implements NonStopConfigRegistry {
  private final ConcurrentMap<NonStopConfigKey, NonStopConfig> allConfigs = new ConcurrentHashMap<NonStopConfigKey, NonStopConfig>();

  @Override
  public void registerForType(NonStopConfig config, ToolkitObjectType... types) {
    for (ToolkitObjectType type : types) {
      allConfigs.put(new NonStopConfigKey(null, type, null), config);
    }
  }

  @Override
  public void registerForInstance(NonStopConfig config, String toolkitTypeName, ToolkitObjectType... types) {
    for (ToolkitObjectType type : types) {
      allConfigs.put(new NonStopConfigKey(null, type, toolkitTypeName), config);
    }
  }

  @Override
  public void registerForTypeMethod(NonStopConfig config, String methodName, ToolkitObjectType... types) {
    for (ToolkitObjectType type : types) {
      allConfigs.put(new NonStopConfigKey(methodName, type, null), config);
    }
  }

  @Override
  public void registerForInstanceMethod(NonStopConfig config, String methodName, String toolkitTypeName,
                                        ToolkitObjectType... types) {
    for (ToolkitObjectType type : types) {
      allConfigs.put(new NonStopConfigKey(methodName, type, toolkitTypeName), config);
    }
  }

  @Override
  public NonStopConfig getConfigForType(ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, null);
    // TODO: instead of returning null should we return a default config
    return allConfigs.get(nonStopConfigKey);
  }

  @Override
  public NonStopConfig getConfigForInstance(String toolkitTypeName, ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, toolkitTypeName);
    NonStopConfig nonStopConfig = allConfigs.get(nonStopConfigKey);
    if (nonStopConfig == null) {
      nonStopConfig = getConfigForType(type);
    }
    return nonStopConfig;
  }

  @Override
  public NonStopConfig getConfigForTypeMethod(String methodName, ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, null);
    NonStopConfig nonStopConfig = allConfigs.get(nonStopConfigKey);
    if (nonStopConfig == null) {
      nonStopConfig = getConfigForType(type);
    }
    return nonStopConfig;
  }

  @Override
  public NonStopConfig getConfigForInstanceMethod(String methodName, String toolkitTypeName, ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, toolkitTypeName);
    NonStopConfig nonStopConfig = allConfigs.get(nonStopConfigKey);

    // try method & type
    if (nonStopConfig == null) {
      nonStopConfigKey = new NonStopConfigKey(methodName, type, null);
      nonStopConfig = allConfigs.get(nonStopConfigKey);
    }

    // try for instance
    if (nonStopConfig == null) {
      nonStopConfig = getConfigForInstance(toolkitTypeName, type);
    }

    return nonStopConfig;
  }

  @Override
  public NonStopConfig deregisterForType(ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, null);
    return allConfigs.remove(nonStopConfigKey);
  }

  @Override
  public NonStopConfig deregisterForInstance(String toolkitTypeName, ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, toolkitTypeName);
    return allConfigs.remove(nonStopConfigKey);
  }

  @Override
  public NonStopConfig deregisterForTypeMethod(String methodName, ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, null);
    return allConfigs.remove(nonStopConfigKey);
  }

  @Override
  public NonStopConfig deregisterForInstanceMethod(String methodName, String toolkitTypeName, ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, toolkitTypeName);
    return allConfigs.remove(nonStopConfigKey);
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
