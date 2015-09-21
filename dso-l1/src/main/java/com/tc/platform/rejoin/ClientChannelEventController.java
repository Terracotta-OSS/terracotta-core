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
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.object.context.PauseContext;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.util.CallStackTrace;

import java.util.concurrent.atomic.AtomicBoolean;

public class ClientChannelEventController {

  private static final TCLogger         LOGGER   = TCLogging.getLogger(ClientChannelEventController.class);

  private final ClientHandshakeManager clientHandshakeManager;
  private final Sink<PauseContext> pauseSink;
  private final AtomicBoolean          shutdown       = new AtomicBoolean(false);
  private final ClientMessageChannel channel;

  /**
   * Creates the event controller and connects it to the given channel.
   */
  public static void connectChannelEventListener(ClientMessageChannel channel, Sink<PauseContext> pauseSink, ClientHandshakeManager clientHandshakeManager) {
    ClientChannelEventController controller = new ClientChannelEventController(channel, pauseSink, clientHandshakeManager);
    channel.addListener(new ChannelEventListenerImpl(controller));
  }
  
  private ClientChannelEventController(ClientMessageChannel channel, Sink<PauseContext> pauseSink, ClientHandshakeManager clientHandshakeManager) {
    this.pauseSink = pauseSink;
    this.clientHandshakeManager = clientHandshakeManager;
    this.channel = channel;
  }

  private void pause() {
    this.pauseSink.addSingleThreaded(new PauseContext(true));
  }

  private void unpause() {
    this.pauseSink.addSingleThreaded(new PauseContext(false));
  }

  public void shutdown() {
    shutdown.set(true);
  }

  private void channelOpened(ChannelEvent event) {
    // no-op
  }

  private void channelConnected(ChannelEvent event) {
    unpause();
  }

  private void channelDisconnected(ChannelEvent event) {
    pause();
  }

  private void channelClosed(ChannelEvent event) {
    requestDisconnect();
  }

  private void channelReconnectionRejected() {
    requestDisconnect();
  }

  private void requestDisconnect() {
    clientHandshakeManager.fireNodeError();
    //Shutdown instead of disconnect as now it should go into disconnected state
    clientHandshakeManager.shutdown(false);
    LOGGER.fatal("Reconnection was rejected from server, but rejoin is not enabled. This client will never be able to join the cluster again.");
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
      LOGGER.info("Got channel event - type: " + event.getType() + ", event: " + event
                      + CallStackTrace.getCallStack());
      if (controller.clientHandshakeManager.isShutdown()) { return; }
      ChannelID eventChannelId = event.getChannelID();
      ClientMessageChannel currentChannel = controller.channel;
      if (eventChannelId != null && !currentChannel.getChannelID().equals(eventChannelId)) {
        LOGGER.info("Ignoring channel event " + event.getType() + " for channel " + eventChannelId
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
          controller.channelReconnectionRejected();
          break;
        default:
          LOGGER.warn("Ignoring unexpeceted channel event " + event.getType() + " for channel " + eventChannelId);
          break;
      }
    }

  }

}
