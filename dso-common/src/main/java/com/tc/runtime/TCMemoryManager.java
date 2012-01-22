/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime;

public interface TCMemoryManager {

  public void checkGarbageCollectors();

  public void registerForMemoryEvents(MemoryEventsListener listener);

  public void unregisterForMemoryEvents(MemoryEventsListener listener);

  public boolean isMonitorOldGenOnly();

  public void shutdown();
}
