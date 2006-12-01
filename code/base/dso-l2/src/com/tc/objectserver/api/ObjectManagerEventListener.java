/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;


/**
 * Interface for those interested in listening to Object Manager events. I'm thinking this event interface should really
 * only be for "low volume" events since there is fair amount of overhead per event. So things like "object looked up",
 * or "cache hit" aren't very good candidates for this interface
 */
public interface ObjectManagerEventListener {

  /**
   * Called after each GC run is complete
   * 
   * @param stats statistics about this collection
   */
  public void garbageCollectionComplete(GCStats stats);

}
