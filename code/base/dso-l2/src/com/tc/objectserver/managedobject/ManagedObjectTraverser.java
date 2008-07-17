/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ManagedObjectTraverser {

  private enum State {PROCESSED, REACHABLE, REQUIRED, LOOKUP_REQUIRED, LOOKUP_REACHABLE}

  private int                        maxReachableObjects;
  private final Map<ObjectID, State> oids = new HashMap<ObjectID, State>();

  public ManagedObjectTraverser(int maxReachableObjects) {
    this.maxReachableObjects = maxReachableObjects;
  }

  public void traverse(Set<ManagedObjectReference> objects) {
    markProcessed(objects, true);
  }

  private void markProcessed(Set<ManagedObjectReference> objects, boolean traverse) {
    for (final ManagedObjectReference ref : objects) {
      ManagedObject mo = ref.getObject();
      oids.put(mo.getID(), State.PROCESSED);
      maxReachableObjects--;
      if (traverse) {
        mo.addObjectReferencesTo(this);
      }
    }
  }

  public Set<ObjectID> getObjectsToLookup() {
    HashSet<ObjectID> oidsToLookup = new HashSet<ObjectID>(oids.size() < 512 ? oids.size() : 512);
    for (final Entry<ObjectID, State> e : oids.entrySet()) {
      State _state = e.getValue();
      if (_state == State.REQUIRED) {
        oidsToLookup.add(e.getKey());
        e.setValue(State.LOOKUP_REQUIRED);
      } else if (maxReachableObjects - oidsToLookup.size() > 0 && _state == State.REACHABLE) {
        oidsToLookup.add(e.getKey());
        e.setValue(State.LOOKUP_REACHABLE);
      }
    }
    return oidsToLookup;
  }

  public Set<ObjectID> getPendingObjectsToLookup(Set<ManagedObjectReference> lookedUpObjects) {
    if(lookedUpObjects.size() > 0) {
      markProcessed(lookedUpObjects, false);
    }
    HashSet<ObjectID> oidsToLookup = new HashSet<ObjectID>(oids.size() < 512 ? oids.size() : 512);
    for (final Entry<ObjectID, State> e : oids.entrySet()) {
      State _state = e.getValue();
      Assert.assertTrue(_state != State.REQUIRED);
      if (_state == State.LOOKUP_REQUIRED) {
        oidsToLookup.add(e.getKey());
      }
    }
    return oidsToLookup;
  }

  public void addRequiredObjectIDs(Set<ObjectID> objectReferences) {
    for (final ObjectID oid : objectReferences) {
      if (oid.isNull()) continue;
      Object state = oids.get(oid);
      if (state == null || state == State.REACHABLE) {
        oids.put(oid, State.REQUIRED);
      }
    }
  }

  public void addReachableObjectIDs(Set<ObjectID> objectReferences) {
    if (maxReachableObjects <= 0) return;
    for (final ObjectID oid : objectReferences) {
      if (oid.isNull()) continue;
      Object state = oids.get(oid);
      if (state == null) {
        oids.put(oid, State.REACHABLE);
      }
    }
  }

}
