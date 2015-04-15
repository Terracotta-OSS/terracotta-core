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
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.PersistentObjectFactory;

import java.io.IOException;
import java.io.ObjectInput;

/**
 * This class represents Maps that can handle partial collections in the L1 side. Currently supported classses are
 * HashMap, LinkedHashMap, Hashtable and Properties. This class should eventually go away once we support partial
 * collections to all maps.
 */
public class PartialMapManagedObjectState extends MapManagedObjectState {

  protected PartialMapManagedObjectState(final long classID, ObjectID id, PersistentObjectFactory factory) {
    super(classID, id, factory);
  }

  protected PartialMapManagedObjectState(final ObjectInput in, PersistentObjectFactory factory) throws IOException {
    super(in, factory);
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addRequiredObjectIDs(getObjectReferencesFrom(this.references.keySet()));
    traverser.addReachableObjectIDs(getObjectReferencesFrom(this.references.values()));
  }

  @Override
  protected void addedReference(final ApplyTransactionInfo applyInfo, final Object o) {
    if (o instanceof ObjectID) {
      // Not adding back reference
      getListener().changed(getId(), null, (ObjectID) o);
    }
  }

  @Override
  public byte getType() {
    return PARTIAL_MAP_TYPE;
  }

  static MapManagedObjectState readFrom(final ObjectInput in, PersistentObjectFactory factory) throws IOException, ClassNotFoundException {
    // make warning go away
    if (false) { throw new ClassNotFoundException(); }
    return new PartialMapManagedObjectState(in, factory);
  }
}
