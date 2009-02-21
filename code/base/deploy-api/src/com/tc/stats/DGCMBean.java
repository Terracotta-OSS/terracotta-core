/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats;

import com.tc.objectserver.api.GCStats;

public interface DGCMBean {

  long getLastCollectionGarbageCount();

  long getLastCollectionElapsedTime();

  GCStats[] getGarbageCollectorStats();

}
