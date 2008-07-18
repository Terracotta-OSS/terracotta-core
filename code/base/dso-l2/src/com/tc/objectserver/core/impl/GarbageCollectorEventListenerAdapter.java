/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.objectserver.core.api.GarbageCollectionInfo;
import com.tc.objectserver.core.api.GarbageCollectorEventListener;

public abstract class GarbageCollectorEventListenerAdapter implements GarbageCollectorEventListener {

  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    //do nothing
  }

  public void garbageCollectorCycleCompleted(GarbageCollectionInfo info) {
   //
  }

  public void garbageCollectorDelete(GarbageCollectionInfo info) {
   //
  }

  public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
    //
  }

  public void garbageCollectorMark(GarbageCollectionInfo info) {
    //
  }

  public void garbageCollectorMarkResults(GarbageCollectionInfo info) {
   //
  }

  public void garbageCollectorPaused(GarbageCollectionInfo info) {
    //
  }

  public void garbageCollectorPausing(GarbageCollectionInfo info) {
    //
  }

  public void garbageCollectorRescue1(GarbageCollectionInfo info) {
    //
  }

  public void garbageCollectorRescue1Results(GarbageCollectionInfo info) {
    //
  }

  public void garbageCollectorRescue2(GarbageCollectionInfo info) {
    //
  }

  public void garbageCollectorRescue2Results(GarbageCollectionInfo info) {
    //
  }

  public void garbageCollectorStart(GarbageCollectionInfo info) {
    //
  }

}
