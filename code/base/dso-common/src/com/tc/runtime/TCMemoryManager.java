/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.runtime;

public interface TCMemoryManager {
  
  public void registerForMemoryEvents(MemoryEventsListener listener);
    
  public void unregisterForMemoryEvents(MemoryEventsListener listener);
    
}
