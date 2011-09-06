/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.cluster.DsoCluster;
import com.tc.injection.annotations.InjectedDsoInstance;

public class ClusterEventsL1Client {
  @InjectedDsoInstance
  private DsoCluster                   cluster;

  private final ClusterEventsTestState state = new ClusterEventsTestState();

  public static void main(final String args[]) {
    new ClusterEventsL1Client();
  }

  public ClusterEventsL1Client() {
    cluster.addClusterListener(state.getListenerForNode(cluster.getCurrentNode()));
    // wait for events a little bit
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      // ignored
    }
  }
}
