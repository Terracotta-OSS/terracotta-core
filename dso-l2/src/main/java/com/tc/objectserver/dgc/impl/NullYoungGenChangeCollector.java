/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;

final class NullYoungGenChangeCollector implements YoungGenChangeCollector {

  public void notifyObjectCreated(ObjectID id) {
    return;
  }

  public void notifyObjectsEvicted(Collection evicted) {
    return;
  }

  public Set addYoungGenCandidateObjectIDsTo(Set set) {
    return set;
  }

  public void notifyObjectInitalized(ObjectID id) {
    return;
  }

  public Set getRememberedSet() {
    return Collections.EMPTY_SET;
  }

  public void removeGarbage(SortedSet ids) {
    return;
  }

  public void startMonitoringChanges() {
    return;
  }

  public void stopMonitoringChanges() {
    return;
  }
}