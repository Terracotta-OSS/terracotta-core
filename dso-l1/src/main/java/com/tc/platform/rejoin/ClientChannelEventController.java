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
import com.tc.object.context.RejoinContext;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.net.DSOClientMessageChannel;

import java.util.concurrent.atomic.AtomicBoolean;

public class ClientChannelEventController {

  // private static final TCLogger logger = TCLogging.getLogger(ClientChannelEventController.class);
  private static final TCLogger             DSO_LOGGER     = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger             CONSOLE_LOGGER = CustomerLogging.getConsoleLogger();

  private final ClientHandshakeManager      clientHandshakeManager;
  private final Sink                        pauseSink;
  private final AtomicBoolean               shutdown       = new AtomicBoolean(false);

  public ClientChannelEventController(DSOClientMessageChannel channel, Sink pauseSink,
                                      ClientHandshakeManager clientHandshakeManager) {
    this.pauseSink = pauseSink;
    this.clientHandshakeManager = clientHandshakeManager;
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
    requestRejoin(event);
  }

  private void channelReconnectionRejected(ChannelEvent event) {
    requestRejoin(event);
  }

  private void requestRejoin(ChannelEvent event) {
    logRejoinStatusMessages(event);
    pauseSink.add(new RejoinContext(event.getChannel()));
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
      // logger.info("Got channel event - type: " + event.getType() + ", event: " + event, new Exception());
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
