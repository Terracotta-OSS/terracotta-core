/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import EDU.oswego.cs.dl.util.concurrent.SyncSet;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.text.PrettyPrinter;

import java.util.Collection;
import java.util.Set;

public interface ManagedObjectPersistor {

  public Set loadRoots();

  public Set loadRootNames();

  public ObjectID loadRootID(String name);
  
  public void addRoot(PersistenceTransaction tx, String name, ObjectID id);

  public ManagedObject loadObjectByID(ObjectID id);

  public long nextObjectIDBatch(int batchSize);

  public SyncSet getAllObjectIDs();

  public void saveObject(PersistenceTransaction tx, ManagedObject managedObject);

  public void saveAllObjects(PersistenceTransaction tx, Collection managed);

  public void deleteAllObjectsByID(PersistenceTransaction tx, Collection ids);
  
  public void prettyPrint(PrettyPrinter out);

}
