/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ValidateObjectsRequestContext;
import com.tc.objectserver.l1.api.InvalidateObjectManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.util.ObjectIDSet;

import java.util.Set;

public class ValidateObjectsHandler extends AbstractEventHandler implements EventHandler {

  private final InvalidateObjectManager invalidateObjMgr;
  private final ManagedObjectStore      objectStore;
  private final ObjectManager           objectManager;

  public ValidateObjectsHandler(InvalidateObjectManager invalidateObjMgr, ObjectManager objectManager,
                                ManagedObjectStore objectStore) {
    this.invalidateObjMgr = invalidateObjMgr;
    this.objectManager = objectManager;
    this.objectStore = objectStore;
  }

  @Override
  public void handleEvent(EventContext context) {
    if (!(context instanceof ValidateObjectsRequestContext)) { throw new AssertionError("Unknown context type : "
                                                                                        + context); }

    final ObjectIDSet evictableObjects = this.objectStore.getAllEvictableObjectIDs();
    final ObjectIDSet allReachables = new ObjectIDSet();
    for (ObjectID evictableID : evictableObjects) {
      addReachablesTo(evictableID, allReachables);
    }
    invalidateObjMgr.validateObjects(allReachables);
  }

  private void addReachablesTo(ObjectID evictableID, ObjectIDSet allReachables) {
    Set oids = objectManager.getObjectReferencesFrom(evictableID, false);
    allReachables.addAll(oids);
  }

}
