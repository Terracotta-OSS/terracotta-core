package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;

import com.tc.object.persistence.api.PersistentMapStore;

import java.util.Map;

/**
 * @author tim
 */
public class PersistentMapStoreImpl implements PersistentMapStore {
  private static String KEY_VALUE_STORAGE_NAME = "persistent_map_store";

  private final KeyValueStorage<String, String> stateMap;

  public PersistentMapStoreImpl(StorageManager storageManager) {
    this.stateMap = storageManager.getKeyValueStorage(KEY_VALUE_STORAGE_NAME, String.class, String.class);
  }

  public static void addConfigTo(Map<String, KeyValueStorageConfig<?, ?>> configMap) {
    configMap.put(KEY_VALUE_STORAGE_NAME, ImmutableKeyValueStorageConfig.builder(String.class, String.class).build());
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
