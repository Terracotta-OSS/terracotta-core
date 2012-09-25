package com.tc.objectserver.persistence.gb;

import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.persistence.gb.gbapi.GBMap;

/**
 * @author tim
 */
public class GBPersistentMapStore implements PersistentMapStore {
  private final GBMap<String, String> stateMap = null;

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
