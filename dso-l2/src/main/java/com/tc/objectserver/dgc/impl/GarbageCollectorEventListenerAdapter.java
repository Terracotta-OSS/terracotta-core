/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.util.ObjectIDSet;

import java.io.Serializable;

public abstract class GarbageCollectorEventListenerAdapter implements GarbageCollectorEventListener, Serializable {

  @Override
  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    // do nothing
  }

  @Override
  public void garbageCollectorCycleCompleted(GarbageCollectionInfo info, ObjectIDSet toDelete) {
    //
  }

  @Override
  public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorMark(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorMarkResults(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorPaused(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorPausing(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorRescue1Complete(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorRescue2Start(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorStart(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorCanceled(GarbageCollectionInfo info) {
    //
  }
}
