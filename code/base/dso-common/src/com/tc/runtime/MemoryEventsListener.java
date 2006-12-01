/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.runtime;

public interface MemoryEventsListener {
  
  public void memoryUsed(MemoryEventType type, MemoryUsage usage);
  
}
