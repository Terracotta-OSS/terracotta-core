/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.impl;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.objectserver.core.api.GarbageCollectionInfo;
import com.tc.objectserver.core.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.core.api.GarbageCollectorEventListener;

import java.util.Iterator;
import java.util.List;

class GarbageCollectionInfoPublisherImpl implements GarbageCollectionInfoPublisher {

  public List garbageCollectionEventListeners = new CopyOnWriteArrayList();

  public void addListener(GarbageCollectorEventListener listener) {
    garbageCollectionEventListeners.add(listener);
  }

  public void fireGCStartEvent(GarbageCollectionInfo info) {
    for (Iterator iter = garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorStart(info);
    }
  }

  public void fireGCMarkEvent(GarbageCollectionInfo info) {
    for (Iterator iter = garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorMark(info);
    }
  }

  public void fireGCMarkResultsEvent(GarbageCollectionInfo info) {
    for (Iterator iter = garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorMarkResults(info);
    }
  }

  public void fireGCRescue1CompleteEvent(GarbageCollectionInfo info) {
    for (Iterator iter = garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorRescue1Complete(info);
    }
  }

  public void fireGCPausingEvent(GarbageCollectionInfo info) {
    for (Iterator iter = garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorPausing(info);
    }
  }

  public void fireGCRescue2StartEvent(GarbageCollectionInfo info) {
    for (Iterator iter = garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorRescue2Start(info);
    }
  }

  public void fireGCPausedEvent(GarbageCollectionInfo info) {
    for (Iterator iter = garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorPaused(info);
    }
  }

  public void fireGCMarkCompleteEvent(GarbageCollectionInfo info) {
    for (Iterator iter = garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorMarkComplete(info);
    }
  }

  public void fireGCDeleteEvent(GarbageCollectionInfo info) {
    for (Iterator iter = garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorDelete(info);
    }
  }

  public void fireGCCycleCompletedEvent(GarbageCollectionInfo info) {
    for (Iterator iter = garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorCycleCompleted(info);
    }
  }

  public void fireGCCompletedEvent(GarbageCollectionInfo info) {
    for (Iterator iter = garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorCompleted(info);
    }
  }
}