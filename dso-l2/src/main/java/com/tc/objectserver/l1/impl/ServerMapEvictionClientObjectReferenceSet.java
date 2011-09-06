/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.api.ObjectReferenceAddListener;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.StripedObjectIDSet;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerMapEvictionClientObjectReferenceSet implements ObjectReferenceAddListener {

  private static final long            REFRESH_INTERVAL_NANO = TimeUnit.MILLISECONDS
                                                                 .toNanos(TCPropertiesImpl
                                                                     .getProperties()
                                                                     .getLong(TCPropertiesConsts.L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL,
                                                                              60000));
  public static final long             MONITOR_INTERVAL_NANO = REFRESH_INTERVAL_NANO * 5;

  private final ClientStateManager     clientStateManager;
  private final TCLogger               logger;
  private final AtomicBoolean          objectReferencesRefreshInProgress;
  private final ReentrantReadWriteLock lock;

  private volatile long                lastRefreshTime;

  // accessed under local lock / thread safe
  private StripedObjectIDSet           liveObjectReferences;
  private ObjectIDSet                  snapshotObjectReferences;
  private boolean                      objectRefAddRegistered;
  private final Timer                  timer;

  public ServerMapEvictionClientObjectReferenceSet(final ClientStateManager clientStateManager) {
    this.clientStateManager = clientStateManager;
    this.logger = TCLogging.getLogger(ServerMapEvictionClientObjectReferenceSet.class);
    this.lock = new ReentrantReadWriteLock();
    this.timer = new Timer();

    this.snapshotObjectReferences = new ObjectIDSet();
    this.liveObjectReferences = new StripedObjectIDSet();
    this.objectReferencesRefreshInProgress = new AtomicBoolean();

    refreshClientObjectReferencesIfNeeded();
    logger.info("ServerMapEviction Client Object References refresh interval "
                + TimeUnit.NANOSECONDS.toSeconds(REFRESH_INTERVAL_NANO) + " seconds.");
  }

  public boolean contains(Object value) {
    boolean found = false;
    refreshClientObjectReferencesIfNeeded();

    this.lock.readLock().lock();
    try {
      found = this.snapshotObjectReferences.contains(value);
      if (!found) {
        found = this.liveObjectReferences.contains(value);
      }
    } finally {
      this.lock.readLock().unlock();
    }

    return found;
  }

  public int size() {
    refreshClientObjectReferencesIfNeeded();
    this.lock.readLock().lock();
    try {
      return this.snapshotObjectReferences.size() + this.liveObjectReferences.size();
    } finally {
      this.lock.readLock().unlock();
    }
  }

  private void refreshClientObjectReferencesIfNeeded() {
    if (((lastRefreshTime + REFRESH_INTERVAL_NANO) < System.nanoTime())
        && objectReferencesRefreshInProgress.compareAndSet(false, true)) {
      this.reinitObjectReferenceSnapshot();
      objectReferencesRefreshInProgress.set(false);
    }
  }

  public void objectReferenceAdded(ObjectID objectID) {
    this.liveObjectReferences.add(objectID);
  }

  public void objectReferencesAdded(Set<ObjectID> objectIDs) {
    this.liveObjectReferences.addAll(objectIDs);
  }

  private void reinitObjectReferenceSnapshot() {
    this.lock.writeLock().lock();
    try {

      this.lastRefreshTime = System.nanoTime();

      // register for new object reference added
      monitorObjectReferenceAddition();

      this.snapshotObjectReferences = new ObjectIDSet();
      this.clientStateManager.addAllReferencedIdsTo(snapshotObjectReferences);
      this.liveObjectReferences = new StripedObjectIDSet();
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  private void monitorObjectReferenceAddition() {

    if (objectRefAddRegistered) return;

    final TimerTask task = new TimerTask() {
      @Override
      public void run() {

        lock.writeLock().lock();
        try {
          if ((lastRefreshTime < (System.nanoTime() - REFRESH_INTERVAL_NANO))) {
            clientStateManager.unregisterObjectReferenceAddListener(ServerMapEvictionClientObjectReferenceSet.this);
            cancel();
            objectRefAddRegistered = false;
          }
        } finally {
          lock.writeLock().unlock();
        }
      }
    };

    this.clientStateManager.registerObjectReferenceAddListener(this);
    this.timer.schedule(task, TimeUnit.NANOSECONDS.toMillis(MONITOR_INTERVAL_NANO));
    objectRefAddRegistered = true;
  }
}
