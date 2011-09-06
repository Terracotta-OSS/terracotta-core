/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public interface ManagedObjectPersistor {

  public void close();

  public Set loadRoots();

  public Set loadRootNames();

  public ObjectID loadRootID(String name);

  public void addRoot(PersistenceTransaction tx, String name, ObjectID id);

  public ManagedObject loadObjectByID(ObjectID id);

  public long nextObjectIDBatch(int batchSize);

  public long currentObjectIDValue();

  public void setNextAvailableObjectID(long startID);

  public void saveObject(PersistenceTransaction tx, ManagedObject managedObject);

  public void saveAllObjects(PersistenceTransaction tx, Collection managed);

  public void deleteAllObjects(SortedSet<ObjectID> ids);

  public Map loadRootNamesToIDs();

  public int getObjectCount();

  public boolean addNewObject(ManagedObject managed);

  boolean containsObject(ObjectID id);

  public void removeAllObjectIDs(SortedSet<ObjectID> ids);

  public ObjectIDSet snapshotObjectIDs();

  public ObjectIDSet snapshotEvictableObjectIDs();

  public ObjectIDSet snapshotMapTypeObjectIDs();

}
