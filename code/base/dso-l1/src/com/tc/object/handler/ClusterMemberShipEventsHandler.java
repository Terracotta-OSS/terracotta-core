/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tcclient.cluster.ClusterInternalEventsContext;
import com.tcclient.cluster.DsoClusterInternal.EVENTS;

public class ClusterMemberShipEventsHandler extends AbstractEventHandler {

  private final Sink clusterEventsHandlerSink;

  public ClusterMemberShipEventsHandler(final Sink sink) {
    this.clusterEventsHandlerSink = sink;
  }

  @Override
  public void handleEvent(EventContext context) throws EventHandlerException {
    if (context instanceof ClusterMembershipMessage) {
      handleClusterMembershipMessage((ClusterMembershipMessage) context);
    } else {
      throw new AssertionError("unknown event type: " + context.getClass().getName());
    }
  }

  private void handleClusterMembershipMessage(final ClusterMembershipMessage cmm) throws EventHandlerException {
    if (cmm.isNodeConnectedEvent()) {
      clusterEventsHandlerSink.add(new ClusterInternalEventsContext(EVENTS.NODE_JOIN, cmm.getNodeId()));
    } else if (cmm.isNodeDisconnectedEvent()) {
      clusterEventsHandlerSink.add(new ClusterInternalEventsContext(EVENTS.NODE_LEFT, cmm.getNodeId()));
    } else {
      throw new EventHandlerException("Unknown event type: " + cmm);
    }
  }

}
