/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;


/**
 * Interface for those interested in listening to Object Manager events. I'm thinking this event interface should really
 * only be for "low volume" events since there is fair amount of overhead per event. So things like "object looked up",
 * or "cache hit" aren't very good candidates for this interface
 */
public interface GCStatsEventListener {

  /**
   * notify the listener that GCStats object has been updated
   * @param stats statistics about this collection
   */
  public void update(GCStats stats);

 
}
