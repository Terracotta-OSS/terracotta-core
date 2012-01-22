/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime;

public interface MemoryEventsListener {

  void memoryUsed(MemoryUsage usage, boolean recommendOffheap);

}
