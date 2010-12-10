/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import java.io.Serializable;

public interface OffheapStats extends Serializable {

  long getOffheapObjectCachedCount();

  /**
   * This will be locked
   */
  long getExactOffheapObjectCachedCount();

  long getOffHeapFaultRate();

  long getOffHeapFlushRate();

  long getOffheapMaxDataSize();

  long getOffheapTotalAllocatedSize();

  long getOffheapObjectAllocatedMemory();

  long getOffheapMapAllocatedMemory();
}
