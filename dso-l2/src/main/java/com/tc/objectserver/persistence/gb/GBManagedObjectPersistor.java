package com.tc.objectserver.persistence.gb;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

/**
 * @author tim
 */
public class GBManagedObjectPersistor implements ManagedObjectPersistor {

  // This should be persistent
  private final KeyValueStorage<String, ObjectID> rootMap;
  private final KeyValueStorage<ObjectID, ManagedObject> objectMap;
  private final GBSequence objectIDSequence;

  private final GBObjectIDSetMaintainer oidSetMaintainer;

  public GBManagedObjectPersistor(KeyValueStorage<String, ObjectID> rootMap, KeyValueStorage<Long, byte[]> objectMap, GBSequence objectIDSequence, final GBObjectIDSetMaintainer oidSetMaintainer) {
    this.rootMap = rootMap;
    this.oidSetMaintainer = oidSetMaintainer;
    this.objectMap = new GBObjectMap(this, objectMap);
    this.objectIDSequence = objectIDSequence;
  }

  public static KeyValueStorageConfig<String, ObjectID> rootMapConfig() {
    return new KeyValueStorageConfigImpl<String, ObjectID>(String.class, ObjectID.class);
  }

  public static KeyValueStorageConfig<Long, byte[]> objectConfig() {
    return GBObjectMap.getConfig();
  }

  @Override
  public void close() {
  }

  @Override
  public Set loadRoots() {
    return new HashSet<ObjectID>(rootMap.values());
  }

  @Override
  public Set loadRootNames() {
    return rootMap.keySet();
  }

  @Override
  public ObjectID loadRootID(String name) {
    ObjectID id = rootMap.get(name);
    return id == null ? ObjectID.NULL_ID : id;
  }

  @Override
  public void addRoot(PersistenceTransaction tx, String name, ObjectID id) {
    rootMap.put(name, id);
  }

  @Override
  public ManagedObject loadObjectByID(ObjectID id) {
    return objectMap.get(id);
  }

  @Override
  public long nextObjectIDBatch(int batchSize) {
    return objectIDSequence.nextBatch(batchSize);
  }

  @Override
  public long currentObjectIDValue() {
    return objectIDSequence.current();
  }

  @Override
  public void setNextAvailableObjectID(long startID) {
    objectIDSequence.setNext(startID);
  }

  @Override
  public void saveObject(PersistenceTransaction tx, ManagedObject managedObject) {
    objectMap.put(managedObject.getID(), managedObject);
    managedObject.setIsDirty(false);
  }

  @Override
  public void saveAllObjects(PersistenceTransaction tx, Collection managed) {
    for (ManagedObject managedObject : (Collection<ManagedObject> ) managed) {
      saveObject(tx, managedObject);
    }
  }

  @Override
  public void deleteAllObjects(SortedSet<ObjectID> ids) {
    objectMap.removeAll(ids);
  }

  @Override
  public Map loadRootNamesToIDs() {
    return asJdkMap(rootMap);
  }

  private <K, V> Map<K, V> asJdkMap(KeyValueStorage<K, V> map) {
    Map<K, V> m = new HashMap<K, V>();
    for (K k : map.keySet()) {
      m.put(k, map.get(k));
    }
    return m;
  }

  @Override
  public int getObjectCount() {
    return (int) objectMap.size();
  }

  @Override
  @Deprecated
  public boolean addNewObject(ManagedObject managed) {
    // This populates the evictable, map type, extant object id sets. They're maintained via our
    // mutation listener.
    return true;
  }

  @Override
  public boolean containsObject(ObjectID id) {
    return objectMap.containsKey(id);
  }

  @Override
  @Deprecated
  public void removeAllObjectIDs(SortedSet<ObjectID> ids) {
    // Handled by the mutation listener like addNewObject();
  }

  @Override
  public ObjectIDSet snapshotObjectIDs() {
    return oidSetMaintainer.objectIDSnapshot();
  }

  @Override
  public ObjectIDSet snapshotEvictableObjectIDs() {
    return oidSetMaintainer.evictableObjectIDSetSnapshot();
  }

  @Override
  @Deprecated
  public ObjectIDSet snapshotMapTypeObjectIDs() {
    // Only appears to be used in tests, probably get rid of this.
    return oidSetMaintainer.mapObjectIDSetSnapshot();
  }
}
