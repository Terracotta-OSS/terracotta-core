/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.terracottatech.config.PersistenceMode;

public class GCConfigurationHelper {
  public interface Parameters {
    public final int     NODE_COUNT                  = 3;
    public final int     LOOP_ITERATION_COUNT        = 1;
    public final int     GARBAGE_COLLECTION_INTERVAL = 10;
    public final boolean GC_ENABLED                  = true;
    public final boolean GC_VERBOSE                  = true;
  }

  private int nodeCount                 = Parameters.NODE_COUNT;
  private int garbageCollectionInterval = Parameters.GARBAGE_COLLECTION_INTERVAL;

  public boolean getGCEnabled() {
    return Parameters.GC_ENABLED;
  }

  public boolean getGCVerbose() {
    return Parameters.GC_VERBOSE;
  }

  public int getGarbageCollectionInterval() {
    return garbageCollectionInterval;
  }

  public void setGarbageCollectionInterval(int garbageCollectionInterval) {
    this.garbageCollectionInterval = garbageCollectionInterval;
  }

  public int getNodeCount() {
    return nodeCount;
  }

  public void setNodeCount(int nodes) {
    nodeCount = nodes;
  }

  public void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    configFactory.setGCEnabled(getGCEnabled());
    configFactory.setGCVerbose(getGCVerbose());
    configFactory.setGCIntervalInSec(getGarbageCollectionInterval());
    configFactory.setPersistenceMode(PersistenceMode.TEMPORARY_SWAP_ONLY);
  }
  
}
