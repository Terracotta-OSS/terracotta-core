/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;

public interface EvictableEntry {

  /**
   * This method computes when this entry will expire relative to "now". If the tti & ttl are 0 then this method returns
   * a positive value that is representative of when the item was last accessed which can then be used to decide which
   * entries can be evicted
   * 
   * @return when values is going to expire relative to "now", a negative number or zero indicates the value is expired.
   * @param now
   * @param ttiSeconds
   * @param ttlSeconds
   */
  public long expiresIn(long now, long ttiSeconds, long ttlSeconds);

  public ObjectID getObjectID();
}
