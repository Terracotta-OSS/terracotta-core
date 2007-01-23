/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.cluster;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.object.msg.ClusterMembershipMessage;

public class ClusterEventHandler extends AbstractEventHandler {

  private final Cluster cluster;

  public ClusterEventHandler(Cluster cluster) {
    this.cluster = cluster;
  }

  public void handleEvent(EventContext context) throws EventHandlerException {
    if (! (context instanceof ClusterMembershipMessage)) throw new EventHandlerException("Unknown context type: " + context.getClass().getName());
    ClusterMembershipMessage cmm = (ClusterMembershipMessage) context;
    
    if (cmm.isNodeConnectedEvent()) {
      cluster.nodeConnected(cmm.getNodeId());
    } else if (cmm.isNodeDisconnectedEvent()) {
      cluster.nodeDisconnected(cmm.getNodeId());
    } else if (cmm.isThisNodeConnected()) {
      cluster.thisNodeConnected(cmm.getNodeId(), cmm.getAllNodeIds());
    } else {
      throw new EventHandlerException("Unknown event type: " + cmm);
    }
  }

}
