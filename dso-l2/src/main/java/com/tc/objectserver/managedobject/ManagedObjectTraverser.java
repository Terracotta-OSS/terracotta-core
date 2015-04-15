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
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.util.Assert;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ManagedObjectTraverser {

  private enum State {
    PROCESSED, REACHABLE, REQUIRED, LOOKUP_REQUIRED, LOOKUP_REACHABLE
  }

  private int                        maxReachableObjects;
  private final Map<ObjectID, State> oids = new HashMap<ObjectID, State>();

  public ManagedObjectTraverser(final int maxReachableObjects) {
    this.maxReachableObjects = maxReachableObjects;
  }

  public void traverse(final Collection<ManagedObject> lookedUpObjects) {
    markProcessed(lookedUpObjects, true);
  }

  private void markProcessed(final Collection<ManagedObject> lookedUpObjects, final boolean traverse) {
    for (final ManagedObject mo : lookedUpObjects) {
      final ManagedObjectReference ref = mo.getReference();
      if (!ref.isReferenced()) { throw new AssertionError(
                                                          "Objects should be marked referenced before calling into this method : "
                                                              + mo); }
      this.oids.put(mo.getID(), State.PROCESSED);
      this.maxReachableObjects--;
      if (traverse) {
        mo.addObjectReferencesTo(this);
      }
    }
  }

  public Set<ObjectID> getObjectsToLookup() {
    final HashSet<ObjectID> oidsToLookup = new HashSet<ObjectID>(this.oids.size() < 512 ? this.oids.size() : 512);
    for (final Entry<ObjectID, State> e : this.oids.entrySet()) {
      final State _state = e.getValue();
      if (_state == State.REQUIRED) {
        oidsToLookup.add(e.getKey());
        e.setValue(State.LOOKUP_REQUIRED);
      } else if (this.maxReachableObjects - oidsToLookup.size() > 0 && _state == State.REACHABLE) {
        oidsToLookup.add(e.getKey());
        e.setValue(State.LOOKUP_REACHABLE);
      }
    }
    return oidsToLookup;
  }

  public ObjectIDSet getPendingObjectsToLookup(final Collection<ManagedObject> lookedUpObjects) {
    if (lookedUpObjects.size() > 0) {
      markProcessed(lookedUpObjects, false);
    }
    final ObjectIDSet oidsToLookup = new BitSetObjectIDSet();
    for (final Entry<ObjectID, State> e : this.oids.entrySet()) {
      final State _state = e.getValue();
      Assert.assertTrue(_state != State.REQUIRED);
      if (_state == State.LOOKUP_REQUIRED) {
        oidsToLookup.add(e.getKey());
      }
    }
    return oidsToLookup;
  }

  public void addRequiredObjectIDs(final Set<ObjectID> objectReferences) {
    for (final ObjectID oid : objectReferences) {
      if (oid.isNull()) {
        continue;
      }
      final Object state = this.oids.get(oid);
      if (state == null || state == State.REACHABLE) {
        this.oids.put(oid, State.REQUIRED);
      }
    }
  }

  public void addReachableObjectIDs(final Set<ObjectID> objectReferences) {
    if (this.maxReachableObjects <= 0) { return; }
    for (final ObjectID oid : objectReferences) {
      if (oid.isNull()) {
        continue;
      }
      final Object state = this.oids.get(oid);
      if (state == null) {
        this.oids.put(oid, State.REACHABLE);
      }
    }
  }

}
