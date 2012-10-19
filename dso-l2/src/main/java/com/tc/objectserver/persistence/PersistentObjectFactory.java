package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;

import com.tc.object.ObjectID;

import java.io.Serializable;

/**
 * @author tim
 */
public class PersistentObjectFactory {
  private static final KeyValueStorageConfig<Serializable, Serializable> mapConfig = new ImmutableKeyValueStorageConfig<Serializable, Serializable>(Serializable.class, Serializable.class);

  private final StorageManager gbManager;

  public PersistentObjectFactory(final StorageManager gbManager) {
    this.gbManager = gbManager;
  }

  public KeyValueStorage<Object, Object> createMap(ObjectID objectID) {
    return (KeyValueStorage) gbManager.createKeyValueStorage(objectID.toString(), mapConfig);
  }

  public KeyValueStorage<Object, Object> getMap(final ObjectID id) {
    return (KeyValueStorage) gbManager.getKeyValueStorage(id.toString(), Serializable.class, Serializable.class);
  }

  public void destroyMap(ObjectID oid) {
    gbManager.destroyKeyValueStorage(oid.toString());
  }
}
