package com.tc.objectserver.persistence;

import org.terracotta.corestorage.StorageManager;

import com.tc.object.persistence.api.PersistentMapStore;

import java.util.Map;

/**
 * @author tim
 */
public class PersistentMapStoreImpl implements PersistentMapStore {
  private final Map<String, String> stateMap;

  public PersistentMapStoreImpl(StorageManager storageManager) {
    this.stateMap = storageManager.getProperties();
  }

  @Override
  public String get(String key) {
    return stateMap.get(key);
  }

  @Override
  public boolean remove(String key) {
    return stateMap.remove(key) != null;
  }

  @Override
  public void put(String key, String value) {
    stateMap.put(key, value);
  }
}
