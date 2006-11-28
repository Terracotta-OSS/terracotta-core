/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.runtime;

public interface MemoryEventsListener {
  
  public void memoryUsed(MemoryEventType type, MemoryUsage usage);
  
}
