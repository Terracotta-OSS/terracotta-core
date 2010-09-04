/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.utils;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class L2CommUtils {
  private static final int MAX_DEFAULT_COMM_THREADS = 16;

  public static int getNumCommWorkerThreads() {
    int def = Math.min(Runtime.getRuntime().availableProcessors() * 2, MAX_DEFAULT_COMM_THREADS);
    return TCPropertiesImpl.getProperties().getInt("l2.tccom.workerthreads", def);
  }

  public static long getMaxDirectMemmoryConsumable() {
    // L2<==L1, L2<==>L2
    int totalCommsThreads = getNumCommWorkerThreads() * 2;
    int maxPossbileMessageBytesSend = (TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.TC_MESSAGE_GROUPING_ENABLED) ? TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.TC_MESSAGE_GROUPING_MAXSIZE_KB) * 1024 : 1);

    // twice of reads and writes; and, max 32m is sufficient
    long totalMemNeeded = totalCommsThreads * maxPossbileMessageBytesSend * 4;
    long sufficientMem = 32 * 1024 * 1024;
    return (totalMemNeeded > sufficientMem ? sufficientMem : totalMemNeeded);
  }
}
