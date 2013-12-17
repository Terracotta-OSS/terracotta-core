/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.config;

import static org.terracotta.toolkit.store.ToolkitConfigFields.MAX_BYTES_LOCAL_HEAP_FIELD_NAME;
import static org.terracotta.toolkit.store.ToolkitConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME;

import org.terracotta.toolkit.config.AbstractConfiguration;
import org.terracotta.toolkit.config.Configuration;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UnclusteredConfiguration extends AbstractConfiguration {
  private static final BiMap<String, String>            conflictingFields = prepareConflictingFields();
  private final ConcurrentHashMap<String, Serializable> map               = new ConcurrentHashMap<String, Serializable>();

  public UnclusteredConfiguration() {
    //
  }

  /**
   * prepare a Immutable BiMap of conflicting fields
   */
  private static BiMap<String, String> prepareConflictingFields() {
    // TODO: move this inside AbstractConfiguration (need to release toolkit-api for that)
    BiMap<String, String> conflictMap = ImmutableBiMap.of(MAX_COUNT_LOCAL_HEAP_FIELD_NAME,
                                                          MAX_BYTES_LOCAL_HEAP_FIELD_NAME);
    return conflictMap;
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

  /**
   * checks if this configuration has any field which is conflicting to name
   */
  public boolean hasConflictingField(String name) {
    String conflictingField = conflictingFields.get(name);
    if (conflictingField == null) {
      conflictingField = conflictingFields.inverse().get(name);
    }
    return conflictingField != null && map.containsKey(conflictingField);
  }

  @Override
  public String toString() {
    return "Configuration [" + this.map + "]";
  }
}
