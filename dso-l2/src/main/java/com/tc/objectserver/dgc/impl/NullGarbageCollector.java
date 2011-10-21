/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.LifeCycleState;

import java.util.Collection;

public class NullGarbageCollector implements GarbageCollector {

  public boolean isPausingOrPaused() {
    return false;
  }

  public boolean isPaused() {
    return false;
  }

  public void notifyReadyToGC() {
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

  public void doGC(GCType type) {
    //
  }

  public void start() {
    // do nothing null
  }

  public void stop() {
    // do nothing null
  }

  public void setState(LifeCycleState st) {
    // do nothing null
  }

  public void addListener(GarbageCollectorEventListener listener) {
    //
  }

  public boolean requestDisableGC() {
    return true;
  }

  public void enableGC() {
    // do nothing null
  }

  public void waitToDisableGC() {
    // do nothing
  }

  public boolean isDisabled() {
    return true;
  }

  public boolean isStarted() {
    return false;
  }

  public void deleteGarbage(DGCResultContext resultContext) {
    //
  }

  public void notifyNewObjectInitalized(ObjectID id) {
    // do nothing null
  }

  public void notifyObjectCreated(ObjectID id) {
    // do nothing null
  }

  public void notifyObjectsEvicted(Collection evicted) {
    // do nothing null
  }

  public boolean requestGCStart() {
    return false;
  }

  public void waitToStartGC() {
    // do nothing
  }

  public void waitToStartInlineGC() {
    // do nothing
  }

  public void setPeriodicEnabled(boolean periodicEnabled) {
    // do nothing
  }

  public boolean isPeriodicEnabled() {
    return false;
  }

  public boolean isDelete() {
    return false;
  }

  public boolean requestGCDeleteStart() {
    return false;
  }
}
