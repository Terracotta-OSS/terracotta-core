package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

import com.tc.object.persistence.api.PersistentMapStore;

/**
 * @author tim
 */
public class PersistentMapStoreImpl implements PersistentMapStore {
  private final KeyValueStorage<String, String> stateMap;

  public PersistentMapStoreImpl(KeyValueStorage<String, String> stateMap) {
    this.stateMap = stateMap;
  }

  public static KeyValueStorageConfig<String, String> config() {
    KeyValueStorageConfig<String, String> config = new KeyValueStorageConfigImpl<String, String>(String.class, String.class);
    config.setKeySerializer(StringSerializer.INSTANCE);
    config.setValueSerializer(StringSerializer.INSTANCE);
    return config;
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
