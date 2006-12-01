/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.ObjectManagerEventListener;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.StoppableThread;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class NullGarbageCollector implements GarbageCollector {

  public Set collect(Filter filter, Collection roots, Set managedObjectIds) {
    return Collections.EMPTY_SET;
  }

  public Set collect(Filter filter, Collection roots, Set managedObjectIds, LifeCycleState state) {
    return Collections.EMPTY_SET;
  }

  public boolean isPausingOrPaused() {
    return false;
  }

  public boolean isPaused() {
    return false;
  }

  public void notifyReadyToGC() {
    return;
  }

  public void blockUntilReadyToGC() {
    return;
  }

  public void requestGCPause() {
    return;
  }

  public void notifyGCComplete() {
    return;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out.print(getClass().getName());
  }

  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    // do nothing null
  }

  public void gc() {
    // do nothing null
  }

  public void addNewReferencesTo(Set rescueIds) {
    // do nothing null
  }

  public void start() {
    // do nothing null
  }

  public void stop() {
    // do nothing null
  }

  public void setState(StoppableThread st) {
    // do nothing null
  }

  public void addListener(ObjectManagerEventListener listener) {
    // do nothing null
  }

  public GCStats[] getGarbageCollectorStats() {
    return null;
  }

}