/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.async.api.Stage;
import com.tc.cluster.Cluster;


public interface ClusterInternal extends Cluster, ClusterInternalEventsGun, ClusterEventsNotifier {
  public static enum ClusterEventType {
    NODE_JOIN("Node Joined"), NODE_LEFT("Node Left"), OPERATIONS_ENABLED("Operations Enabled"), OPERATIONS_DISABLED(
        "Operations Disabled"), NODE_REJOINED("Node Rejoined"), NODE_ERROR("NODE ERROR");

    private final String name;

    private ClusterEventType(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public void init(Stage<ClusterInternalEventsContext> clusterEventsStage);

  public void shutdown();

  public void cleanup();
}