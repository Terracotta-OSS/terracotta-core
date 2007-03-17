/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.text.PrettyPrinter;
import com.tc.util.SyncObjectIdSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ManagedObjectPersistor {

  public Set loadRoots();

  public Set loadRootNames();

  public ObjectID loadRootID(String name);
  
  public void addRoot(PersistenceTransaction tx, String name, ObjectID id);

  public ManagedObject loadObjectByID(ObjectID id);

  public long nextObjectIDBatch(int batchSize);

  public void setNextAvailableObjectID(long startID);
  
  public SyncObjectIdSet getAllObjectIDs();

  public void saveObject(PersistenceTransaction tx, ManagedObject managedObject);

  public void saveAllObjects(PersistenceTransaction tx, Collection managed);

  public void deleteAllObjectsByID(PersistenceTransaction tx, Collection ids);
  
  public void prettyPrint(PrettyPrinter out);

  public Map loadRootNamesToIDs();

}
