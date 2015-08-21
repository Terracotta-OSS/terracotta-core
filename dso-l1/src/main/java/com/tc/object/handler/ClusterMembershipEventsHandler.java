/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.net.ClientID;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tcclient.cluster.ClusterInternalEventsGun;

public class ClusterMembershipEventsHandler<EC> extends AbstractEventHandler<EC> {

  private final ClusterInternalEventsGun clusterEventsGun;

  public ClusterMembershipEventsHandler(ClusterInternalEventsGun ClusterEventsGun) {
    this.clusterEventsGun = ClusterEventsGun;
  }

  @Override
  public void handleEvent(EC context) throws EventHandlerException {
    if (context instanceof ClusterMembershipMessage) {
      handleClusterMembershipMessage((ClusterMembershipMessage) context);
    } else {
      throw new AssertionError("unknown event type: " + context.getClass().getName());
    }
  }

  private void handleClusterMembershipMessage(ClusterMembershipMessage cmm) throws EventHandlerException {
    if (cmm.getProductId().isInternal()) {
      // don't fire events for internal products.
      return;
    }
    if (cmm.isNodeConnectedEvent()) {
      clusterEventsGun.fireNodeJoined((ClientID)cmm.getNodeId());
    } else if (cmm.isNodeDisconnectedEvent()) {
      clusterEventsGun.fireNodeLeft((ClientID)cmm.getNodeId());
    } else {
      throw new EventHandlerException("Unknown event type: " + cmm);
    }
  }

}
