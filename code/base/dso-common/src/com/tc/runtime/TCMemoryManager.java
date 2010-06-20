/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime;

public interface TCMemoryManager {

  public void checkGarbageCollectors();

  public void registerForMemoryEvents(MemoryEventsListener listener);

  public void unregisterForMemoryEvents(MemoryEventsListener listener);

  public boolean isMonitorOldGenOnly();

  public void shutdown();
}
