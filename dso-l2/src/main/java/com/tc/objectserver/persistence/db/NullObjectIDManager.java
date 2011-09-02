/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.ObjectIDSet;
import com.tc.util.SyncObjectIdSet;

import java.util.SortedSet;

public class NullObjectIDManager implements ObjectIDManager {

  public Runnable getObjectIDReader(final SyncObjectIdSet rv) {
    return returnDummy(rv);
  }

  private Runnable returnDummy(final SyncObjectIdSet rv) {
    // a dummy one, just stop populating and return
    return new Runnable() {
      public void run() {
        rv.stopPopulating(new ObjectIDSet());
        return;
      }
    };
  }

  public Runnable getMapsObjectIDReader(final SyncObjectIdSet rv) {
    return returnDummy(rv);
  }

  public Runnable getEvictableObjectIDReader(final SyncObjectIdSet rv) {
    return returnDummy(rv);
  }

  public boolean deleteAll(final PersistenceTransaction tx, final SortedSet<ObjectID> oidsToDelete,
                           final SyncObjectIdSet extantMapTypeOidSet, final SyncObjectIdSet extantEvictableOidSet) {
    return true;
  }

  public boolean put(final PersistenceTransaction tx, final ManagedObject mo) {
    return true;
  }

  public boolean putAll(final PersistenceTransaction tx, final SortedSet<ManagedObject> managedObjects) {
    return true;
  }

  public void close() {
    // Nothing to close
  }
}
