/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.context.GCResultContext;
import com.tc.text.PrettyPrintable;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.StoppableThread;

import java.util.Collection;
import java.util.Set;

public interface GarbageCollector extends PrettyPrintable {

  public void enableGC();

  public boolean disableGC();

  public boolean isDisabled();

  public boolean isPausingOrPaused();

  public boolean isPaused();

  /**
   * Called by object manager. Notifies the garbage collector that it's ok to perform GC.
   */
  public void notifyReadyToGC();

  /**
   * Request to pause when the system state stabilizes
   */
  public void requestGCPause();

  /**
   * Called by the GC thread. Notifies the garbage collector that GC is complete.
   */
  public void notifyGCComplete();
  
  /**
   * @param traverser Determines whether or not to traverse a given tree node.
   * @param roots
   * @param managedObjects
   * @return An set on the objects that can be deleted
   */
  public ObjectIDSet collect(Filter traverser, Collection roots, ObjectIDSet managedObjectIds);

  public ObjectIDSet collect(Filter traverser, Collection roots, ObjectIDSet managedObjectIds, LifeCycleState state);

  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference);

  public void gc();

  public void addNewReferencesTo(Set rescueIds);

  public void start();

  public void stop();

  public boolean isStarted();

  public void setState(StoppableThread st);
  
  public void addListener(GarbageCollectorEventListener listener);
  
  public boolean deleteGarbage(GCResultContext resultContext);

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

  /**
   * This is the method called on the collector to do YoungGen collection, collectors that are not interested in doing
   * YoungGen collection could ignore this call.
   */
  public void gcYoung();

}