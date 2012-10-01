package com.tc.objectserver.persistence.gb;

import com.tc.gbapi.impl.GBOnHeapMapConfig;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.gbapi.GBManager;
import com.tc.gbapi.GBMap;
import com.tc.gbapi.GBMapConfig;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.ObjectIDSet;

import java.util.*;

/**
 * @author tim
 */
public class GBManagedObjectPersistor implements ManagedObjectPersistor {

  // This should be persistent
  private final GBMap<String, ObjectID> rootMap;
  private final GBMap<ObjectID, ManagedObject> objectMap;
  private final GBSequence objectIDSequence;

  private final GBObjectIDSetMaintainer oidSetMaintainer = new GBObjectIDSetMaintainer();

  public GBManagedObjectPersistor(GBMap<String, ObjectID> rootMap, GBMap<ObjectID, ManagedObject> objectMap, GBSequence objectIDSequence) {
    this.rootMap = rootMap;
    this.objectMap = objectMap;
    this.objectIDSequence = objectIDSequence;
  }

  public static GBMapConfig<String, ObjectID> rootMapConfig() {
    return new GBOnHeapMapConfig<String, ObjectID>(String.class, ObjectID.class);
  }

  public static GBMapConfig<ObjectID, ManagedObject> objectConfig(GBManager gbManager) {
    return new GBOnHeapMapConfig<ObjectID, ManagedObject>(ObjectID.class,
                                                          ManagedObject.class);
  }

  @Override
  public void close() {
  }

  @Override
  public Set loadRoots() {
    return new HashSet(rootMap.values());
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

  private <K, V> Map<K, V> asJdkMap(GBMap<K, V> map) {
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
