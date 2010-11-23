/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tcclient.cluster.ClusterInternalEventsContext;
import com.tcclient.cluster.DsoClusterInternal;

/**
 * Handler firing the dso cluster internal events to the listeners
 */
public class ClusterInternalEventsHandler extends AbstractEventHandler {

  private final DsoClusterInternal dsoCluster;

  public ClusterInternalEventsHandler(final DsoClusterInternal cluster) {
    this.dsoCluster = cluster;
  }

  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof ClusterInternalEventsContext) {
      handleClusterInternalEvents((ClusterInternalEventsContext) context);
    } else {
      throw new AssertionError("Unknown Context " + context);
    }
  }

  private void handleClusterInternalEvents(ClusterInternalEventsContext context) {

    switch (context.getEventType()) {
      case THIS_NODE_JOIN:
        this.dsoCluster.fireThisNodeJoined(context.getEventNodeID(), context.getOtherNodeIDs());
        break;

      case THIS_NODE_LEFT:
        this.dsoCluster.fireThisNodeLeft();
        break;

      case NODE_JOIN:
        this.dsoCluster.fireNodeJoined(context.getEventNodeID());
        break;

      case NODE_LEFT:
        this.dsoCluster.fireNodeLeft(context.getEventNodeID());
        break;

      case OPERATIONS_ENABLED:
        this.dsoCluster.fireOperationsEnabled();
        break;

      case OPERATIONS_DISABLED:
        this.dsoCluster.fireOperationsDisabled();
        break;

      default:
        throw new AssertionError("Unknown cluster event : " + context);
    }

  }
}
