package com.tc.objectserver.persistence.gb;

import com.tc.object.ObjectID;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageFactory;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

/**
 * @author tim
 */
public class GBPersistentObjectFactory {
  private final StorageManager gbManager;

  public GBPersistentObjectFactory(final StorageManager gbManager) {
    this.gbManager = gbManager;
  }

  public KeyValueStorage<Object, Object> createMap(ObjectID objectID) {
    return gbManager.createKeyValueStorage(objectID.toString(), new KeyValueStorageConfigImpl<Object, Object>(Object.class, Object.class));
  }

  public KeyValueStorage<Object, Object> getMap(final ObjectID id) {
    return gbManager.getKeyValueStorage(id.toString(), Object.class, Object.class);
  }

  public void destroyMap(ObjectID oid) {
    gbManager.destroyKeyValueStorage(oid.toString());
  }
}
