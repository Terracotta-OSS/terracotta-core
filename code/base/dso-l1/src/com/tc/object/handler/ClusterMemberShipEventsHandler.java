/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tcclient.cluster.DsoClusterInternal;

public class ClusterMemberShipEventsHandler extends AbstractEventHandler {
  
  private final DsoClusterInternal dsoCluster;

  public ClusterMemberShipEventsHandler(final DsoClusterInternal dsoCluster) {
    this.dsoCluster = dsoCluster;
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
      dsoCluster.fireNodeJoined(cmm.getNodeId());
    } else if (cmm.isNodeDisconnectedEvent()) {
      dsoCluster.fireNodeLeft(cmm.getNodeId());
    } else {
      throw new EventHandlerException("Unknown event type: " + cmm);
    }
  }

}
