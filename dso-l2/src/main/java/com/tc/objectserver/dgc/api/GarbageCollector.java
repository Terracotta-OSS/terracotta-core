/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.text.PrettyPrintable;
import com.tc.util.State;
import com.tc.util.concurrent.LifeCycleState;

import java.util.Collection;

public interface GarbageCollector extends PrettyPrintable {

  public static enum GCType {
    YOUNG_GEN_GC, FULL_GC, INLINE_CLEANUP_GC
  }

  public static final State GC_DISABLED = new State("GC_DISABLED");
  public static final State GC_RUNNING  = new State("GC_RUNNING");
  public static final State GC_SLEEP    = new State("GC_SLEEP");
  public static final State GC_PAUSING  = new State("GC_PAUSING");
  public static final State GC_PAUSED   = new State("GC_PAUSED");
  public static final State GC_DELETE   = new State("GC_DELETE");

  public boolean requestGCStart();

  /**
   * Used by inline GC to delete objects.
   */
  public void waitToStartInlineGC();

  public void waitToStartGC();

  public void enableGC();

  public void waitToDisableGC();

  public boolean requestDisableGC();

  public boolean isDisabled();

  public boolean isPausingOrPaused();

  public boolean isPaused();

  public boolean isDelete();

  /**
   * Called by object manager. Notifies the garbage collector that it's ok to perform GC.
   */
  public void notifyReadyToGC();

  /**
   * Request to pause when the system state stabilizes
   */
  public void requestGCPause();

  public boolean requestGCDeleteStart();

  /**
   * Called by the GC thread. Notifies the garbage collector that GC is complete.
   */
  public void notifyGCComplete();

  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference);

  public void doGC(GCType type);

  /**
   * This method is called when the server transitions from PASSIVE to ACTIVE
   */
  public void start();

  public void stop();

  public boolean isStarted();

  public void setPeriodicEnabled(final boolean periodicEnabled);

  public boolean isPeriodicEnabled();

  public void setState(LifeCycleState st);

  public void addListener(GarbageCollectorEventListener listener);

  public void deleteGarbage(DGCResultContext resultContext);

  /**
   * Whenever a new object is created, this method is called from the Object Manager. This is used for YoungGen
   * collection, collectors that are not interested in doing young generation collection could ignore this call.
   */
  public void notifyObjectCreated(ObjectID id);

  /**
   * When a new object is initialized for the first time, this method is called from the Object Manager. This is used
   * for YoungGen collection, collectors that are not interested in doing young generation collection could ignore this
   * call.
   */
  public void notifyNewObjectInitalized(ObjectID id);

  /**
   * Whenever objects are evicted from memory, this method is called from the Object Manager. The collection contains
   * the list of ManagedObjects evicted. This is used for Young generation collection, collectors that are not
   * interested in doing YoungGen collection could ignore this call.
   */
  public void notifyObjectsEvicted(Collection evicted);

}