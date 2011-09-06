/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.SyncObjectIdSet;

import java.util.SortedSet;

public interface ObjectIDManager {

  public void close();

  public boolean put(PersistenceTransaction tx, ManagedObject mo) throws TCDatabaseException;

  public boolean putAll(PersistenceTransaction tx, SortedSet<ManagedObject> managedObjects) throws TCDatabaseException;

  public boolean deleteAll(PersistenceTransaction tx, SortedSet<ObjectID> oidsToDelete,
                           SyncObjectIdSet extantMapTypeOidSet, SyncObjectIdSet extantEvictableOidSet)
      throws TCDatabaseException;

  public Runnable getObjectIDReader(SyncObjectIdSet objectIDSet);

  public Runnable getMapsObjectIDReader(SyncObjectIdSet objectIDSet);

  public Runnable getEvictableObjectIDReader(SyncObjectIdSet objectIDSet);

}
