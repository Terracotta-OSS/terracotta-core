/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.terracottatech.config.PersistenceMode;

public class GCConfigurationHelper {
  public interface Parameters {
    public final int     NODE_COUNT                  = 3;
    public final int     LOOP_ITERATION_COUNT        = 1;
    public final int     GARBAGE_COLLECTION_INTERVAL = 10;
    public final boolean GC_ENABLED                  = true;
    public final boolean GC_VERBOSE                  = true;
  }

  private int node_count = Parameters.NODE_COUNT;

  public boolean getGCEnabled() {
    return Parameters.GC_ENABLED;
  }

  public boolean getGCVerbose() {
    return Parameters.GC_VERBOSE;
  }

  public int getGarbageCollectionInterval() {
    return Parameters.GARBAGE_COLLECTION_INTERVAL;
  }

  public int getNodeCount() {
    return node_count;
  }

  public void setNodeCount(int nodes) {
    node_count = nodes;
  }

  public void setupConfig(TestTVSConfigurationSetupManagerFactory configFactory) {
    configFactory.setGCEnabled(getGCEnabled());
    configFactory.setGCVerbose(getGCVerbose());
    configFactory.setGCIntervalInSec(getGarbageCollectionInterval());
    configFactory.setPersistenceMode(PersistenceMode.TEMPORARY_SWAP_ONLY);
  }
}
