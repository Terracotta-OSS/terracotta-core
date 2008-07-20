/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;
import com.tc.util.SyncObjectIdSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public interface ManagedObjectPersistor {

  public Set loadRoots();

  public Set loadRootNames();

  public ObjectID loadRootID(String name);

  public void addRoot(PersistenceTransaction tx, String name, ObjectID id);

  public ManagedObject loadObjectByID(ObjectID id);

  public long nextObjectIDBatch(int batchSize);

  public void setNextAvailableObjectID(long startID);

  public SyncObjectIdSet getAllObjectIDs();

  public SyncObjectIdSet getAllMapsObjectIDs();

  public void saveObject(PersistenceTransaction tx, ManagedObject managedObject);

  public void saveAllObjects(PersistenceTransaction tx, Collection managed);

  public void deleteAllObjectsByID(PersistenceTransaction tx, SortedSet<ObjectID> ids);

  public Map loadRootNamesToIDs();

  public int getObjectCount();

  public boolean addNewObject(ObjectID id);

  boolean containsObject(ObjectID id);

  public void removeAllObjectsByID(SortedSet<ObjectID> ids);

  public ObjectIDSet snapshotObjects();

  public boolean containsMapType(ObjectID id);

  public boolean addMapTypeObject(ObjectID id);

  public void removeAllMapTypeObject(Collection ids);

}
