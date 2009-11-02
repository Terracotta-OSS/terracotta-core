/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.utils;

import com.tc.properties.TCPropertiesImpl;

public class L2CommUtils {
  private static final int MAX_DEFAULT_COMM_THREADS = 16;

  public static int getNumCommWorkerThreads() {
    int def = Math.min(Runtime.getRuntime().availableProcessors() * 2, MAX_DEFAULT_COMM_THREADS);
    return TCPropertiesImpl.getProperties().getInt("l2.tccom.workerthreads", def);
  }

}
