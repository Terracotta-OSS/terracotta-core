/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

import com.tc.async.api.Sink;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.context.PauseContext;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.runtime.TCMemoryManager;
import com.tcclient.cluster.DsoClusterInternalEventsGun;

import java.util.concurrent.atomic.AtomicBoolean;

public class ClientChannelEventController {

  private static final TCLogger             DSO_LOGGER     = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger             CONSOLE_LOGGER = CustomerLogging.getConsoleLogger();

  private final RejoinManagerInternal       rejoinManager;
  private final ClientHandshakeManager      clientHandshakeManager;
  private final TCMemoryManager             tcMemManager;
  private final Sink                        pauseSink;
  private final DsoClusterInternalEventsGun dsoClusterEventsGun;
  private final AtomicBoolean               shutdown       = new AtomicBoolean(false);

  public ClientChannelEventController(DSOClientMessageChannel channel, Sink pauseSink,
                                      RejoinManagerInternal rejoinManager,
                                      ClientHandshakeManager clientHandshakeManager,
                                      DsoClusterInternalEventsGun dsoClusterEventsGun, TCMemoryManager tcMemManager) {
    this.pauseSink = pauseSink;
    this.rejoinManager = rejoinManager;
    this.clientHandshakeManager = clientHandshakeManager;
    this.dsoClusterEventsGun = dsoClusterEventsGun;
    this.tcMemManager = tcMemManager;
    channel.addListener(new ChannelEventListenerImpl(this));
  }

  private void pause(NodeID remoteNodeId) {
    this.pauseSink.add(new PauseContext(true, remoteNodeId));
  }

  private void unpause(NodeID remoteNodeId) {
    this.pauseSink.add(new PauseContext(false, remoteNodeId));
  }

  public void shutdown() {
    shutdown.set(true);
  }

  private void channelOpened(ChannelEvent event) {
    // no-op
  }

  private void channelConnected(ChannelEvent event) {
    unpause(event.getChannel().getRemoteNodeID());
  }

  private void channelDisconnected(ChannelEvent event) {
    pause(event.getChannel().getRemoteNodeID());
  }

  private void channelClosed(ChannelEvent event) {
    clientHandshakeManager.disconnected(event.getChannel().getRemoteNodeID());
    // MNK-2410: initiate rejoin on transport close too
    initiateRejoin(event);
  }

  private void channelReconnectionRejected(ChannelEvent event) {
    initiateRejoin(event);
  }

  private void initiateRejoin(ChannelEvent event) {
    if (rejoinManager.isRejoinEnabled()) {
      if (true) {
        // TODO: remove me
        oldRejoinBehavior(event);
        return;
      }
      rejoinManager.doRejoin(event.getChannel());
    }
  }

  private void oldRejoinBehavior(ChannelEvent event) {
    logRejoinStatusMessages(event);
    // MNK-2771: stop memory monitor before firing node left
    tcMemManager.shutdown();
    clientHandshakeManager.shutdown();

    if (!this.shutdown.get()) {
      this.dsoClusterEventsGun.fireThisNodeLeft();
    }
  }

  private static void logRejoinStatusMessages(final ChannelEvent event) {
    ChannelID channelID = event.getChannelID();
    String msg = (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT) ? "Channel " + channelID + " closed."
        : "Reconnection rejected event fired, caused by " + channelID;
    CONSOLE_LOGGER.info(msg);
    DSO_LOGGER.info(msg);
    DSO_LOGGER.info("Shutting down clientHandshakeManager...");
  }

  private static class ChannelEventListenerImpl implements ChannelEventListener {

    private final ClientChannelEventController controller;

    public ChannelEventListenerImpl(ClientChannelEventController controller) {
      this.controller = controller;
    }

    @Override
    public void notifyChannelEvent(ChannelEvent event) {
      final NodeID remoteNodeId = event.getChannel().getRemoteNodeID();
      if (GroupID.ALL_GROUPS.equals(remoteNodeId)) { throw new AssertionError("Recd event for Group Channel : " + event); }
      switch (event.getType()) {
        case TRANSPORT_CONNECTED_EVENT:
          controller.channelConnected(event);
          break;
        case TRANSPORT_DISCONNECTED_EVENT:
          controller.channelDisconnected(event);
          break;
        case CHANNEL_CLOSED_EVENT:
          controller.channelClosed(event);
          break;
        case CHANNEL_OPENED_EVENT:
          controller.channelOpened(event);
          break;
        case TRANSPORT_RECONNECTION_REJECTED_EVENT:
          controller.channelReconnectionRejected(event);
          break;
      }
    }

  }

}
