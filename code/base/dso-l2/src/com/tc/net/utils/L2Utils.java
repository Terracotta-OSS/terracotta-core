/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.utils;

import com.tc.bytes.TCByteBufferFactory;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class L2Utils {
  private static final int MAX_DEFAULT_COMM_THREADS            = 16;
  private static final int MAX_DEFAULT_STAGE_THREADS           = 16;
  public static final long MIN_COMMS_DIRECT_MEMORY_REQUIREMENT = 4 * 1024 * 1024;  // 4MiB
  public static final long MAX_COMMS_DIRECT_MEMORY_REQUIREMENT = 256 * 1024 * 1024; // 256MiB

  public static int getOptimalCommWorkerThreads() {
    int def = Math.min(Runtime.getRuntime().availableProcessors() * 2, MAX_DEFAULT_COMM_THREADS);
    return TCPropertiesImpl.getProperties().getInt("l2.tccom.workerthreads", def);
  }

  public static int getOptimalStageWorkerThreads() {
    int def = Math.min(Runtime.getRuntime().availableProcessors() * 2, MAX_DEFAULT_STAGE_THREADS);
    return TCPropertiesImpl.getProperties().getInt("l2.seda.stage.workerthreads", def);
  }

  /**
   * Calculates max possible direct memory consumption by TC Communication system. In fact, TC Comms can ask for more
   * direct byte buffers than computed here if the buffer pool is fully used up, but its rare though.
   * 
   * @return long - maximum consumable direct memory byte buffers in bytes by the comms system.
   */
  public static long getMaxDirectMemmoryConsumable() {

    // L2<==L1, L2<==>L2
    final int totalCommsThreads = getOptimalCommWorkerThreads() * 2;
    final boolean poolingEnabled = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.TC_BYTEBUFFER_POOLING_ENABLED);
    final int directMemoryCommonPool = (TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.TC_BYTEBUFFER_COMMON_POOL_MAXCOUNT, 3000));
    final int directMemoryThreadLocalPool = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.TC_BYTEBUFFER_THREADLOCAL_POOL_MAXCOUNT, 2000);

    long totalDirectMemeoryNeeded;
    if (poolingEnabled) {
      totalDirectMemeoryNeeded = (totalCommsThreads * directMemoryThreadLocalPool * TCByteBufferFactory.FIXED_BUFFER_SIZE)
                                 + (directMemoryCommonPool * TCByteBufferFactory.FIXED_BUFFER_SIZE);
    } else {
      int maxPossbileMessageBytesSend = (TCPropertiesImpl.getProperties()
          .getBoolean(TCPropertiesConsts.TC_MESSAGE_GROUPING_ENABLED) ? TCPropertiesImpl.getProperties()
          .getInt(TCPropertiesConsts.TC_MESSAGE_GROUPING_MAXSIZE_KB) * 1024 : 1024);
      totalDirectMemeoryNeeded = totalCommsThreads * maxPossbileMessageBytesSend * 4;
    }

    totalDirectMemeoryNeeded = (totalDirectMemeoryNeeded < MIN_COMMS_DIRECT_MEMORY_REQUIREMENT ? MIN_COMMS_DIRECT_MEMORY_REQUIREMENT
        : totalDirectMemeoryNeeded);
    return (totalDirectMemeoryNeeded > MAX_COMMS_DIRECT_MEMORY_REQUIREMENT ? MAX_COMMS_DIRECT_MEMORY_REQUIREMENT
        : totalDirectMemeoryNeeded);
  }
}
