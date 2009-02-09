/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.cluster.DsoClusterInternal;
import com.tc.object.msg.ClusterMembershipMessage;

public class ClusterEventsHandler extends AbstractEventHandler {

  private final DsoClusterInternal cluster;

  public ClusterEventsHandler(final DsoClusterInternal cluster) {
    this.cluster = cluster;
  }

  @Override
  public void handleEvent(final EventContext context) throws EventHandlerException {
    if (context instanceof ClusterMembershipMessage) {
      handleClusterMembershipMessage((ClusterMembershipMessage) context);
    } else {
      throw new AssertionError("unknown event type: " + context.getClass().getName());
    }
  }

  private void handleClusterMembershipMessage(final ClusterMembershipMessage cmm) throws EventHandlerException {
    if (cmm.isNodeConnectedEvent()) {
      cluster.fireNodeJoined(cmm.getNodeId());
    } else if (cmm.isNodeDisconnectedEvent()) {
      cluster.fireNodeLeft(cmm.getNodeId());
    } else {
      throw new EventHandlerException("Unknown event type: " + cmm);
    }
  }

  @Override
  public synchronized void initialize(final ConfigurationContext context) {
    super.initialize(context);
  }

}
