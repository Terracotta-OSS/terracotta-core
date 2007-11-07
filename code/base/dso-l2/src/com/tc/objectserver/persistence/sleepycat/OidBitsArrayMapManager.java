/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.OperationStatus;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.SyncObjectIdSet;

import java.util.Set;

public interface OidBitsArrayMapManager {

  public Runnable createObjectIdReader(SyncObjectIdSet set);

  public OperationStatus oidPut(PersistenceTransaction tx, ObjectID objectId) throws DatabaseException;

  public OperationStatus oidPutAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException;

  public OperationStatus oidDeleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException;
}
