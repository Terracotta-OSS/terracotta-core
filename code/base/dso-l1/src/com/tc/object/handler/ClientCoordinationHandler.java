/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.context.PauseContext;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tcclient.cluster.DsoClusterInternal;

public class ClientCoordinationHandler extends AbstractEventHandler {

  private ClientHandshakeManager   handshakeManager;
  private final DsoClusterInternal dsoCluster;

  public ClientCoordinationHandler(final DsoClusterInternal dsoCluster) {
    this.dsoCluster = dsoCluster;
  }

  @Override
  public void handleEvent(final EventContext context) throws EventHandlerException {
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

  private void handlePauseContext(final PauseContext ctxt) {
    if (ctxt.getIsPause()) {
      handshakeManager.disconnected(ctxt.getRemoteNode());
    } else {
      handshakeManager.connected(ctxt.getRemoteNode());
    }
  }

  private void handleClientHandshakeAckMessage(final ClientHandshakeAckMessage handshakeAck) {
    handshakeManager.acknowledgeHandshake(handshakeAck);
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

  @Override
  public synchronized void initialize(final ConfigurationContext context) {
    super.initialize(context);
    this.handshakeManager = ((ClientConfigurationContext) context).getClientHandshakeManager();
  }

}
