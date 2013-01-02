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

  @Override
  public void notifyObjectCreated(ObjectID id) {
    return;
  }

  @Override
  public void notifyObjectsEvicted(Collection evicted) {
    return;
  }

  @Override
  public Set addYoungGenCandidateObjectIDsTo(Set set) {
    return set;
  }

  @Override
  public void notifyObjectInitalized(ObjectID id) {
    return;
  }

  @Override
  public Set getRememberedSet() {
    return Collections.EMPTY_SET;
  }

  @Override
  public void removeGarbage(SortedSet ids) {
    return;
  }

  @Override
  public void startMonitoringChanges() {
    return;
  }

  @Override
  public void stopMonitoringChanges() {
    return;
  }
}