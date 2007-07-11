/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.cluster.Cluster;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.context.PauseContext;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClusterMembershipMessage;

public class ClientCoordinationHandler extends AbstractEventHandler {

  private ClientHandshakeManager handshakeManager;
  private final Cluster          cluster;

  public ClientCoordinationHandler(Cluster cluster) {
    this.cluster = cluster;
  }

  public void handleEvent(EventContext context) throws EventHandlerException {
    // this instanceof stuff is yucky, but these are very low volume events

    if (context instanceof ClusterMembershipMessage) {
      handleClusterMembershipMessage((ClusterMembershipMessage) context);
    } else if (context instanceof ClientHandshakeAckMessage) {
      handleClientHandshakeAckMessage((ClientHandshakeAckMessage) context);
    } else if (context instanceof PauseContext) {
      handlePauseContext((PauseContext) context);
    } else {
      throw new AssertionError("unknown event type: " + context.getClass().getName());
    }
  }

  private void handlePauseContext(PauseContext ctxt) {
    if (ctxt.getIsPause()) {
      handshakeManager.pause();
    } else {
      handshakeManager.unpause();
    }
  }

  private void handleClientHandshakeAckMessage(ClientHandshakeAckMessage handshakeAck) {
    handshakeManager.acknowledgeHandshake(handshakeAck);
  }

  private void handleClusterMembershipMessage(ClusterMembershipMessage cmm) throws EventHandlerException {
    if (cmm.isNodeConnectedEvent()) {
      cluster.nodeConnected(cmm.getNodeId());
    } else if (cmm.isNodeDisconnectedEvent()) {
      cluster.nodeDisconnected(cmm.getNodeId());
    } else {
      throw new EventHandlerException("Unknown event type: " + cmm);
    }
  }

  public synchronized void initialize(ConfigurationContext context) {
    super.initialize(context);
    this.handshakeManager = ((ClientConfigurationContext) context).getClientHandshakeManager();
  }

}
