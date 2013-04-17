/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.config;

import org.terracotta.toolkit.config.AbstractConfiguration;
import org.terracotta.toolkit.config.Configuration;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UnclusteredConfiguration extends AbstractConfiguration {

  private final ConcurrentHashMap<String, Serializable> map = new ConcurrentHashMap<String, Serializable>();

  public UnclusteredConfiguration() {
    //
  }

  public UnclusteredConfiguration(Configuration configuration) {
    for (String key : configuration.getKeys()) {
      this.map.put(key, configuration.getObjectOrNull(key));
    }
  }

  @Override
  public Serializable getObjectOrNull(String name) {
    return map.get(name);
  }

  @Override
  public void internalSetConfigMapping(String name, Serializable value) {
    map.put(name, value);
  }

  @Override
  public Set<String> getKeys() {
    return this.map.keySet();
  }

  @Override
  public String toString() {
    return "Configuration [" + this.map + "]";
  }
}
