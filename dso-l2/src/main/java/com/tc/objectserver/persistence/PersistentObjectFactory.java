package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

import com.tc.object.ObjectID;

/**
 * @author tim
 */
public class PersistentObjectFactory {
  private static final KeyValueStorageConfig<Object, Object> mapConfig;
  static {
    mapConfig = new KeyValueStorageConfigImpl<Object, Object>(Object.class, Object.class);
    mapConfig.setKeySerializer(LiteralSerializer.INSTANCE);
    mapConfig.setValueSerializer(LiteralSerializer.INSTANCE);
  }

  private final StorageManager gbManager;

  public PersistentObjectFactory(final StorageManager gbManager) {
    this.gbManager = gbManager;
  }

  public KeyValueStorage<Object, Object> createMap(ObjectID objectID) {
    return gbManager.createKeyValueStorage(objectID.toString(), mapConfig);
  }

  public KeyValueStorage<Object, Object> getMap(final ObjectID id) {
    return gbManager.getKeyValueStorage(id.toString(), Object.class, Object.class);
  }

  public void destroyMap(ObjectID oid) {
    gbManager.destroyKeyValueStorage(oid.toString());
  }
}
