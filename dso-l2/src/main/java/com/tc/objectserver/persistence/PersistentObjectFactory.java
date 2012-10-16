package com.tc.objectserver.persistence;

import com.tc.object.ObjectID;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;

/**
 * @author tim
 */
public class PersistentObjectFactory {
  private static final KeyValueStorageConfig<Object, Object> mapConfig = new ImmutableKeyValueStorageConfig<Object, Object>(Object.class, Object.class, LiteralSerializer.INSTANCE, LiteralSerializer.INSTANCE);

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
