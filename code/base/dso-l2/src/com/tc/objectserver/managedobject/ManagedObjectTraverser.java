/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class ManagedObjectTraverser {

  private static final String PROCESSED        = "PROCESSED";
  private static final String REACHABLE        = "REACHABLE";
  private static final String REQUIRED         = "REQUIRED";
  private static final String LOOKUP_REQUIRED  = "LOOKUP_REQUIRED";
  private static final String LOOKUP_REACHABLE = "LOOKUP_REACHABLE";

  private int                 maxReachableObjects;
  private final Map           oids             = new HashMap();

  public ManagedObjectTraverser(int maxReachableObjects) {
    this.maxReachableObjects = maxReachableObjects;
  }

  public void traverse(Set objects) {
    markProcessed(objects, true);
  }

  private void markProcessed(Set objects, boolean traverse) {
    for (Iterator i = objects.iterator(); i.hasNext();) {
      ManagedObjectReference ref = (ManagedObjectReference) i.next();
      ManagedObject mo = ref.getObject();
      oids.put(mo.getID(), PROCESSED);
      maxReachableObjects--;
      if (traverse) {
        mo.addObjectReferencesTo(this);
      }
    }
  }

  public Set getObjectsToLookup() {
    HashSet oidsToLookup = new HashSet(oids.size() < 512 ? oids.size() : 512);
    for (Iterator i = oids.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      Object _state = e.getValue();
      if (_state == REQUIRED) {
        oidsToLookup.add(e.getKey());
        e.setValue(LOOKUP_REQUIRED);
      } else if (maxReachableObjects - oidsToLookup.size() > 0 && _state == REACHABLE) {
        oidsToLookup.add(e.getKey());
        e.setValue(LOOKUP_REACHABLE);
      }
    }
    return oidsToLookup;
  }

  public Set getPendingObjectsToLookup(Set lookedUpObjects) {
    if(lookedUpObjects.size() > 0) {
      markProcessed(lookedUpObjects, false);
    }
    HashSet oidsToLookup = new HashSet(oids.size() < 512 ? oids.size() : 512);
    for (Iterator i = oids.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      Object _state = e.getValue();
      Assert.assertTrue(_state != REQUIRED);
      if (_state == LOOKUP_REQUIRED) {
        oidsToLookup.add(e.getKey());
      }
    }
    return oidsToLookup;
  }

  public void addRequiredObjectIDs(Set objectReferences) {
    for (Iterator i = objectReferences.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      if (oid.isNull()) continue;
      Object state = oids.get(oid);
      if (state == null || state == REACHABLE) {
        oids.put(oid, REQUIRED);
      }
    }
  }

  public void addReachableObjectIDs(Set objectReferences) {
    if (maxReachableObjects <= 0) return;
    for (Iterator i = objectReferences.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      if (oid.isNull()) continue;
      Object state = oids.get(oid);
      if (state == null) {
        oids.put(oid, REACHABLE);
      }
    }
  }

}
