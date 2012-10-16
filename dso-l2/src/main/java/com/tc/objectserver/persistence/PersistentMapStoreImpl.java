package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;

import com.tc.object.persistence.api.PersistentMapStore;
import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;

/**
 * @author tim
 */
public class PersistentMapStoreImpl implements PersistentMapStore {
  private final KeyValueStorage<String, String> stateMap;

  public PersistentMapStoreImpl(KeyValueStorage<String, String> stateMap) {
    this.stateMap = stateMap;
  }

  public static KeyValueStorageConfig<String, String> config() {
    return new ImmutableKeyValueStorageConfig<String, String>(String.class, String.class);
  }

  @Override
  public String get(String key) {
    return stateMap.get(key);
  }

  @Override
  public boolean remove(String key) {
    return stateMap.remove(key);
  }

  @Override
  public void put(String key, String value) {
    stateMap.put(key, value);
  }
}
