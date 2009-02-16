/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;

public interface YoungGenChangeCollector {

  public final YoungGenChangeCollector NULL_YOUNG_CHANGE_COLLECTOR = new NullYoungGenChangeCollector();

  public void notifyObjectsEvicted(Collection evicted);

  public void startMonitoringChanges();

  public void stopMonitoringChanges();

  public void removeGarbage(SortedSet ids);

  public Set getRememberedSet();

  public void notifyObjectCreated(ObjectID id);

  public void notifyObjectInitalized(ObjectID id);

  public Set addYoungGenCandidateObjectIDsTo(Set set);
}
