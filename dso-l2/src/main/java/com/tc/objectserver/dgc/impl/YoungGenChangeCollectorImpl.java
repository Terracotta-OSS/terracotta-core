/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;

final class YoungGenChangeCollectorImpl implements YoungGenChangeCollector {

  /* Used in the youngGenObjectIDs Map */
  private static final State UNINITALIZED         = new State("UNINITIALIZED");
  private static final State INITALIZED           = new State("INITIALIZED");

  /* Used for the object state */
  private static final State MONITOR_CHANGES      = new State("MONITOR-CHANGES");
  private static final State DONT_MONITOR_CHANGES = new State("DONT-MONITOR-CHANGES");

  private final Map          youngGenObjectIDs    = new HashMap();
  private final Set          rememberedSet        = new ObjectIDSet();
  private final Set          evictedIDAtGcSet     = new ObjectIDSet();

  private State              state                = DONT_MONITOR_CHANGES;

  public synchronized Set addYoungGenCandidateObjectIDsTo(Set set) {
    for (Iterator i = this.youngGenObjectIDs.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      if (e.getValue() == INITALIZED) {
        set.add(e.getKey());
      }
    }
    return set;
  }

  public synchronized Set getRememberedSet() {
    return new ObjectIDSet(this.rememberedSet);
  }

  public synchronized void notifyObjectCreated(ObjectID id) {
    Object oldState = this.youngGenObjectIDs.put(id, UNINITALIZED);
    if (oldState != null) { throw new AssertionError(id + " is already present in " + oldState); }
  }

  public synchronized void notifyObjectInitalized(ObjectID id) {
    Object oldState = this.youngGenObjectIDs.put(id, INITALIZED);
    if (oldState != UNINITALIZED) { throw new AssertionError(id + " is not in " + UNINITALIZED + " but in " + oldState); }
  }

  public synchronized void notifyObjectsEvicted(Collection evicted) {
    for (Iterator i = evicted.iterator(); i.hasNext();) {
      ManagedObject mo = (ManagedObject) i.next();
      ObjectID id = mo.getID();
      removeReferencesTo(id);
      Set references = mo.getObjectReferences();
      references.retainAll(this.youngGenObjectIDs.keySet());
      this.rememberedSet.addAll(references);
    }
  }

  private void removeReferencesTo(ObjectID id) {
    if (this.state == DONT_MONITOR_CHANGES) {
      this.youngGenObjectIDs.remove(id);
      /**
       * XXX:: We don't want to remove inward reference to Young Gen Objects that are just faulted out of cache
       * (becoming OldGen object) while the DGC is running. If we did it will lead to GCing valid reachable objects
       * since YoungGen only looks us the objects in memory.
       * <p>
       * This seems counter-intuitive to not remove inward pointers when in MONITOR_CHANGES state, but if you think of
       * removing inward references as forgetting the fact that a reference existed, then not removing the reference is
       * Monitoring the changes.
       */
      this.rememberedSet.remove(id);
    } else {
      evictedIDAtGcSet.add(id);
    }
  }

  public synchronized void removeGarbage(SortedSet ids) {
    for (Iterator i = ids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      removeReferencesTo(oid);
    }
  }

  public synchronized void startMonitoringChanges() {
    Assert.assertTrue(this.state == DONT_MONITOR_CHANGES);
    this.state = MONITOR_CHANGES;
  }

  public synchronized void stopMonitoringChanges() {
    Assert.assertTrue(this.state == MONITOR_CHANGES);
    this.state = DONT_MONITOR_CHANGES;

    // remove reaped objectIDs at last DGC
    // and reset remembered set to the latest set of Young Gen IDs
    for (Iterator i = evictedIDAtGcSet.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      youngGenObjectIDs.remove(oid);
      rememberedSet.remove(oid);
    }
    evictedIDAtGcSet.clear();
  }
}