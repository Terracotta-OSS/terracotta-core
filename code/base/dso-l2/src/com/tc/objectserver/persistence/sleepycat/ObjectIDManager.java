/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.OperationStatus;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.SyncObjectIdSet;

import java.util.Set;

public interface ObjectIDManager {

  public OperationStatus put(PersistenceTransaction tx, ObjectID objectID) throws DatabaseException;

  public void prePutAll(Set<ObjectID> oidSet, ObjectID objectID);
  
  public OperationStatus putAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException;

  public OperationStatus deleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException;
  
  public Runnable getObjectIDReader(SyncObjectIdSet rv);
}
