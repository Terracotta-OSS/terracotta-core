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
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;

import java.util.Map;

public class ObjectManagerLookupResultsImpl implements ObjectManagerLookupResults {

  private final Map<ObjectID, ManagedObject> objects;
  private final ObjectIDSet                  lookupPendingObjectIDs;
  private final ObjectIDSet                  missingObjectIDs;

  public ObjectManagerLookupResultsImpl(Map<ObjectID, ManagedObject> objects, ObjectIDSet lookupPendingObjectIDs,
                                        ObjectIDSet missingObjectIDs) {
    this.objects = objects;
    this.lookupPendingObjectIDs = lookupPendingObjectIDs;
    this.missingObjectIDs = missingObjectIDs;
  }

  @Override
  public Map<ObjectID, ManagedObject> getObjects() {
    return this.objects;
  }

  @Override
  public ObjectIDSet getLookupPendingObjectIDs() {
    return lookupPendingObjectIDs;
  }

  @Override
  public ObjectIDSet getMissingObjectIDs() {
    return missingObjectIDs;
  }
}
