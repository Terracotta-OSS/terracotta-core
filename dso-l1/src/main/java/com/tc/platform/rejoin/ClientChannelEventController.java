/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.object.context.PauseContext;
import com.tc.object.context.RejoinContext;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.util.CallStackTrace;

import java.util.concurrent.atomic.AtomicBoolean;

public class ClientChannelEventController {

  private static final TCLogger        DSO_LOGGER = TCLogging.getLogger(ClientChannelEventController.class);

  private final ClientHandshakeManager clientHandshakeManager;
  private final Sink                   pauseSink;
  private final AtomicBoolean          shutdown       = new AtomicBoolean(false);
  private final RejoinManager          rejoinManager;
  private final DSOClientMessageChannel channel;

  public ClientChannelEventController(DSOClientMessageChannel channel, Sink pauseSink,
                                      ClientHandshakeManager clientHandshakeManager, RejoinManager rejoinManager) {
    this.pauseSink = pauseSink;
    this.clientHandshakeManager = clientHandshakeManager;
    this.rejoinManager = rejoinManager;
    this.channel = channel;
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
    // MNK-2410: initiate rejoin on transport close too
    requestRejoin(event);
  }

  private void channelReconnectionRejected(ChannelEvent event) {
    requestRejoin(event);
  }

  private void requestRejoin(ChannelEvent event) {
    clientHandshakeManager.reconnectionRejected();
    if (rejoinManager.isRejoinEnabled()) {
      logRejoinStatusMessages(event);
      pauseSink.add(new RejoinContext(channel.channel()));
    } else {
      DSO_LOGGER
          .fatal("Reconnection was rejected by the L2, but rejoin is not enabled. This client will never be able to join the cluster again.");
      DSO_LOGGER.info("Rejoin request ignored as rejoin is NOT enabled");
    }
  }

  private static void logRejoinStatusMessages(final ChannelEvent event) {
    String msg = (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT) ? "Channel " + event.getChannel()
                                                                              + " closed."
        : "Reconnection rejected event fired, caused by " + event;
    DSO_LOGGER.info(msg);
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
      DSO_LOGGER.info("Got channel event - type: " + event.getType() + ", event: " + event
                      + CallStackTrace.getCallStack());
      ChannelID eventChannelId = event.getChannelID();
      ClientMessageChannel currentChannel = controller.channel.channel();
      if (eventChannelId != null && !currentChannel.getChannelID().equals(eventChannelId)) {
        DSO_LOGGER.info("Ignoring channel event " + event.getType() + " for channel " + eventChannelId
                        + " as currentChannel " + currentChannel.getChannelID());
        return;
      }
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
