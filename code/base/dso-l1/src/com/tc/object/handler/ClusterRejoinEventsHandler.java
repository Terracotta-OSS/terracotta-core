/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tcclient.cluster.ClusterInternalEventsContext;
import com.tcclient.cluster.DsoClusterInternal;

/**
 * Handler firing the dso cluster rejoin(ThisNodeLeft) events to the listeners
 */
public class ClusterRejoinEventsHandler extends AbstractEventHandler {

  private final DsoClusterInternal dsoCluster;

  public ClusterRejoinEventsHandler(final DsoClusterInternal cluster) {
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
      case THIS_NODE_LEFT:
        this.dsoCluster.fireThisNodeLeft();
        break;
      default:
        throw new AssertionError("Unknown cluster event : " + context);
    }

  }
}
