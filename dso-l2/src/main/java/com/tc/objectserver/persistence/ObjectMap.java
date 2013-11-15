package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorageMutationListener;
import org.terracotta.corestorage.StorageManager;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ManagedObjectSerializer;
import com.tc.objectserver.managedobject.ManagedObjectStateSerializer;
import com.tc.objectserver.managedobject.SerializedClusterObjectState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author tim
 */
class ObjectMap implements KeyValueStorage<ObjectID, ManagedObject> {
  private static final String NODE_OBJECT_DB = "node_object_db";
  private static final String LEAF_OBJECT_DB = "leaf_object_db";

  private final KeyValueStorage<Long, byte[]> nodeObjects;
  private final KeyValueStorage<Long, byte[]> leafObjects;
  private final ManagedObjectSerializer serializer;

  ObjectMap(ManagedObjectPersistor persistor, StorageManager storageManager) {
    this.nodeObjects = storageManager.getKeyValueStorage(NODE_OBJECT_DB, Long.class, byte[].class);
    this.leafObjects = storageManager.getKeyValueStorage(LEAF_OBJECT_DB, Long.class, byte[].class);
    this.serializer = new ManagedObjectSerializer(new ManagedObjectStateSerializer(), persistor);
  }

  public static void addConfigTo(Map<String, KeyValueStorageConfig<?, ?>> configMap, KeyValueStorageMutationListener<Long, byte[]> listener,
                                 final StorageManagerFactory storageManagerFactory) {
    configMap.put(NODE_OBJECT_DB, storageManagerFactory.wrapObjectDBConfig(ImmutableKeyValueStorageConfig.builder(Long.class, byte[].class).listener(listener), 
            StorageManagerFactory.Type.NODE));
    configMap.put(LEAF_OBJECT_DB, storageManagerFactory.wrapObjectDBConfig(ImmutableKeyValueStorageConfig.builder(Long.class, byte[].class).listener(listener), 
            StorageManagerFactory.Type.LEAF));
  }

  @Override
  public Set<ObjectID> keySet() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Collection<ManagedObject> values() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public long size() {
    return nodeObjects.size() + leafObjects.size();
  }

  @Override
  public void put(final ObjectID key, final ManagedObject value) {
    put(key, value, (byte) 0);
  }

  @Override
  public void put(final ObjectID key, final ManagedObject value, byte metadata) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      ObjectOutput oo = new ObjectOutputStream(byteArrayOutputStream);
      try {
        serializer.serializeTo(value, oo);
      } finally {
        oo.close();
      }
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    if ( value.getManagedObjectState() instanceof SerializedClusterObjectState  ) {
        leafObjects.put(key.toLong(), byteArrayOutputStream.toByteArray(), metadata);
    } else {
        nodeObjects.put(key.toLong(), byteArrayOutputStream.toByteArray(), metadata);
  }
  }

  @Override
  public ManagedObject get(final ObjectID key) {
    boolean leaf = true;
    byte[] data = leafObjects.get(key.toLong());
    
    if (data == null) {
      data = nodeObjects.get(key.toLong());
      leaf = false;
    }
    if ( data == null ) {
      return null;
    }
    try {
      ObjectInput oi = new ObjectInputStream(new ByteArrayInputStream(data));
      return (ManagedObject)serializer.deserializeFrom(oi);
    } catch (ObjectNotFoundException e) {
      // Clean up the backing map if the object winds up missing (see MNK-5031)
      if ( leaf ) {
        leafObjects.remove(key.toLong());
      } else {
        nodeObjects.remove(key.toLong());
      }
      return null;
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

    @Override
    public boolean remove(final ObjectID key) {
        if ( !leafObjects.remove(key.toLong()) ) {
            return nodeObjects.remove(key.toLong());
    }
        return true;
    }

    @Override
    public void removeAll(final Collection<ObjectID> keys) {
        for (ObjectID key : keys) {
            remove(key);
        }
    }

    @Override
    public boolean containsKey(final ObjectID key) {
        if ( !leafObjects.containsKey(key.toLong()) ) {
            return nodeObjects.containsKey(key.toLong());
    }
        return true;
    }

    @Override
    public void clear() {
        leafObjects.clear();
        nodeObjects.clear();
    }
}
