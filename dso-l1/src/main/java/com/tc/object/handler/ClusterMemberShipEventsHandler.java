/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tcclient.cluster.DsoClusterInternalEventsGun;

public class ClusterMemberShipEventsHandler extends AbstractEventHandler {

  private final DsoClusterInternalEventsGun dsoClusterEventsGun;

  public ClusterMemberShipEventsHandler(final DsoClusterInternalEventsGun dsoClusterEventsGun) {
    this.dsoClusterEventsGun = dsoClusterEventsGun;
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
      dsoClusterEventsGun.fireNodeJoined(cmm.getNodeId());
    } else if (cmm.isNodeDisconnectedEvent()) {
      dsoClusterEventsGun.fireNodeLeft(cmm.getNodeId());
    } else {
      throw new EventHandlerException("Unknown event type: " + cmm);
    }
  }

}
