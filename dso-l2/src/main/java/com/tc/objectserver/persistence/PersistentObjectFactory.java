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
  private static final KeyValueStorageConfig<Serializable, Serializable> MAP_CONFIG = new ImmutableKeyValueStorageConfig<Serializable, Serializable>(Serializable.class, Serializable.class);

  private final StorageManager storageManager;

  public PersistentObjectFactory(final StorageManager storageManager) {
    this.storageManager = storageManager;
  }

  public synchronized KeyValueStorage<Object, Object> getMap(ObjectID objectID, final boolean create) {
    KeyValueStorage map = storageManager.getKeyValueStorage(objectID.toString(), Serializable.class, Serializable.class);
    if (map == null) {
      if (create) {
        map = (KeyValueStorage) storageManager.createKeyValueStorage(objectID.toString(), MAP_CONFIG);
      } else {
        throw new AssertionError("Map for object id " + objectID + " not found.");
      }
    }
    return map;
  }

  public synchronized void destroyMap(ObjectID oid) {
    storageManager.destroyKeyValueStorage(oid.toString());
  }
}
