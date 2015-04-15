/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ValidateObjectsRequestContext;
import com.tc.objectserver.impl.PersistentManagedObjectStore;
import com.tc.objectserver.l1.api.InvalidateObjectManager;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.util.Set;

public class ValidateObjectsHandler extends AbstractEventHandler {

  private final InvalidateObjectManager invalidateObjMgr;
  private final PersistentManagedObjectStore      objectStore;
  private final ObjectManager           objectManager;

  public ValidateObjectsHandler(InvalidateObjectManager invalidateObjMgr, ObjectManager objectManager,
                                PersistentManagedObjectStore objectStore) {
    this.invalidateObjMgr = invalidateObjMgr;
    this.objectManager = objectManager;
    this.objectStore = objectStore;
  }

  @Override
  public void handleEvent(EventContext context) {
    if (!(context instanceof ValidateObjectsRequestContext)) { throw new AssertionError("Unknown context type : "
                                                                                        + context); }

    final ObjectIDSet evictableObjects = this.objectStore.getAllEvictableObjectIDs();
    final ObjectIDSet allReachables = new BitSetObjectIDSet();
    for (ObjectID evictableID : evictableObjects) {
      addReachablesTo(evictableID, allReachables);
    }
    invalidateObjMgr.validateObjects(allReachables);
  }

  private void addReachablesTo(ObjectID evictableID, ObjectIDSet allReachables) {
    Set<ObjectID> oids = objectManager.getObjectReferencesFrom(evictableID, false);
    allReachables.addAll(oids);
  }

}
