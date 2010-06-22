/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

public interface EvictableEntry {

  public boolean canEvict(int ttiSeconds, int ttlSeconds);

}
