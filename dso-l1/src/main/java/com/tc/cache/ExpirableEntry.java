/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cache;

public interface ExpirableEntry {

  /**
   * Determine when this entry will expire based on the tti/ttl passed in. Entries with custom tti/ttl can chose to 
   * ignore the passed in values.
   * 
   * @return The timestamp (in seconds since the epoch) when this value will expire. May return Integer.MAX_VALUE if it
   *         will never expire.
   */
  int expiresAt(int tti, int ttl);
  
}
