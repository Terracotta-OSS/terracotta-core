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

public class NullGarbageCollector implements GarbageCollector {

  @Override
  public boolean isPausingOrPaused() {
    return false;
  }

  @Override
  public boolean isPaused() {
    return false;
  }

  @Override
  public void notifyReadyToGC() {
    return;
  }

  @Override
  public void requestGCPause() {
    return;
  }

  @Override
  public void notifyGCComplete() {
    return;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out.print(getClass().getName());
  }

  @Override
  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    // do nothing null
  }

  @Override
  public void doGC(GCType type) {
    //
  }

  @Override
  public void start() {
    // do nothing null
  }

  @Override
  public void stop() {
    // do nothing null
  }

  @Override
  public void setState(LifeCycleState st) {
    // do nothing null
  }

  @Override
  public void addListener(GarbageCollectorEventListener listener) {
    //
  }

  @Override
  public boolean requestDisableGC() {
    return true;
  }

  @Override
  public void enableGC() {
    // do nothing null
  }

  @Override
  public void waitToDisableGC() {
    // do nothing
  }

  @Override
  public boolean isDisabled() {
    return true;
  }

  @Override
  public boolean isStarted() {
    return false;
  }

  @Override
  public void deleteGarbage(DGCResultContext resultContext) {
    //
  }

  @Override
  public boolean requestGCStart() {
    return false;
  }

  @Override
  public void waitToStartGC() {
    // do nothing
  }

  @Override
  public void waitToStartInlineGC() {
    // do nothing
  }

  @Override
  public void setPeriodicEnabled(boolean periodicEnabled) {
    // do nothing
  }

  @Override
  public boolean isPeriodicEnabled() {
    return false;
  }

  @Override
  public boolean isDelete() {
    return false;
  }

  @Override
  public boolean requestGCDeleteStart() {
    return false;
  }
}
