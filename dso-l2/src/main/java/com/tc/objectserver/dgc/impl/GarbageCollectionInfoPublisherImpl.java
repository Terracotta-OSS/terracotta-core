/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.util.ObjectIDSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GarbageCollectionInfoPublisherImpl implements GarbageCollectionInfoPublisher {

  private static final List<GarbageCollectorEventListener> EMPTY_LIST                              = Collections
                                                                                                       .emptyList();
  /**
   * Can't add any listeners to this one
   */
  public static final GarbageCollectionInfoPublisher       NULL_GARBAGE_COLLECCTION_INFO_PUBLISHER = new GarbageCollectionInfoPublisherImpl(
                                                                                                                                            EMPTY_LIST);

  private final List<GarbageCollectorEventListener>        garbageCollectionEventListeners;

  public GarbageCollectionInfoPublisherImpl() {
    this(new CopyOnWriteArrayList<GarbageCollectorEventListener>());
  }

  private GarbageCollectionInfoPublisherImpl(List<GarbageCollectorEventListener> listeners) {
    this.garbageCollectionEventListeners = listeners;
  }

  @Override
  public void addListener(GarbageCollectorEventListener listener) {
    this.garbageCollectionEventListeners.add(listener);
  }

  @Override
  public void removeListener(GarbageCollectorEventListener listener) {
    this.garbageCollectionEventListeners.remove(listener);
  }

  @Override
  public void fireGCStartEvent(GarbageCollectionInfo info) {
    for (Iterator iter = this.garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorStart(info);
    }
  }

  @Override
  public void fireGCMarkEvent(GarbageCollectionInfo info) {
    for (Iterator iter = this.garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorMark(info);
    }
  }

  @Override
  public void fireGCMarkResultsEvent(GarbageCollectionInfo info) {
    for (Iterator iter = this.garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorMarkResults(info);
    }
  }

  @Override
  public void fireGCRescue1CompleteEvent(GarbageCollectionInfo info) {
    for (Iterator iter = this.garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorRescue1Complete(info);
    }
  }

  @Override
  public void fireGCPausingEvent(GarbageCollectionInfo info) {
    for (Iterator iter = this.garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorPausing(info);
    }
  }

  @Override
  public void fireGCRescue2StartEvent(GarbageCollectionInfo info) {
    for (Iterator iter = this.garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorRescue2Start(info);
    }
  }

  @Override
  public void fireGCPausedEvent(GarbageCollectionInfo info) {
    for (Iterator iter = this.garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorPaused(info);
    }
  }

  @Override
  public void fireGCMarkCompleteEvent(GarbageCollectionInfo info) {
    for (Iterator iter = this.garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorMarkComplete(info);
    }
  }

  @Override
  public void fireGCCycleCompletedEvent(GarbageCollectionInfo info, ObjectIDSet toDelete) {
    for (Iterator iter = this.garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorCycleCompleted(info, toDelete);
    }
  }

  @Override
  public void fireGCCompletedEvent(GarbageCollectionInfo info) {
    for (Iterator iter = this.garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorCompleted(info);
    }
  }

  @Override
  public void fireGCCanceledEvent(GarbageCollectionInfo info) {
    for (Iterator iter = this.garbageCollectionEventListeners.iterator(); iter.hasNext();) {
      GarbageCollectorEventListener listener = (GarbageCollectorEventListener) iter.next();
      listener.garbageCollectorCanceled(info);
    }
  }
}