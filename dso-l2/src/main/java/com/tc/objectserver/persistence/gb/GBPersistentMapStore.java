package com.tc.objectserver.persistence.gb;

import com.tc.object.persistence.api.PersistentMapStore;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

/**
 * @author tim
 */
public class GBPersistentMapStore implements PersistentMapStore {
  private final KeyValueStorage<String, String> stateMap;

  public GBPersistentMapStore(KeyValueStorage<String, String> stateMap) {
    this.stateMap = stateMap;
  }

  public static KeyValueStorageConfig<String, String> config() {
    return new KeyValueStorageConfigImpl<String, String>(String.class, String.class);
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
