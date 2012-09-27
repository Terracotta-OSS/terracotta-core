package com.tc.objectserver.persistence.gb;

import com.tc.gbapi.GBMap;
import com.tc.gbapi.GBMapConfig;
import com.tc.gbapi.GBMapMutationListener;
import com.tc.gbapi.GBSerializer;
import com.tc.object.persistence.api.PersistentMapStore;

/**
 * @author tim
 */
public class GBPersistentMapStore implements PersistentMapStore {
  private final GBMap<String, String> stateMap;

  public GBPersistentMapStore(GBMap<String, String> stateMap) {
    this.stateMap = stateMap;
  }

  public static GBMapConfig<String, String> config() {
    return new GBMapConfig<String, String>() {
      @Override
      public void setKeySerializer(GBSerializer<String> serializer) {
      }

      @Override
      public void setValueSerializer(GBSerializer<String> serializer) {
      }

      @Override
      public Class<String> getKeyClass() {
        return null;
      }

      @Override
      public Class<String> getValueClass() {
        return null;
      }

      @Override
      public void addListener(GBMapMutationListener<String, String> listener) {
      }
    };
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
