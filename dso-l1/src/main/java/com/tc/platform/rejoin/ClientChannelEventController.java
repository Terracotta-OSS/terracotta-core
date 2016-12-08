/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.platform.rejoin;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.util.CallStackTrace;

import java.util.concurrent.atomic.AtomicBoolean;

public class ClientChannelEventController {

  private static final TCLogger         LOGGER   = TCLogging.getLogger(ClientChannelEventController.class);

  private final ClientHandshakeManager clientHandshakeManager;
  private final AtomicBoolean          shutdown       = new AtomicBoolean(false);
  private final ClientMessageChannel channel;

  /**
   * Creates the event controller and connects it to the given channel.
   */
  public static void connectChannelEventListener(ClientMessageChannel channel, ClientHandshakeManager clientHandshakeManager) {
    ClientChannelEventController controller = new ClientChannelEventController(channel, clientHandshakeManager);
    channel.addListener(new ChannelEventListenerImpl(controller));
  }
  
  private ClientChannelEventController(ClientMessageChannel channel, ClientHandshakeManager clientHandshakeManager) {
    this.clientHandshakeManager = clientHandshakeManager;
    this.channel = channel;
  }

  public void shutdown() {
    shutdown.set(true);
  }

  private void channelOpened(ChannelEvent event) {
    LOGGER.debug("channel opened:" + event.getChannelID());
  }

  private void channelConnected(ChannelEvent event) {
    LOGGER.debug("channel connected:" + event.getChannelID());
    this.clientHandshakeManager.connected();
  }

  private void channelDisconnected(ChannelEvent event) {
    LOGGER.debug("channel disconnected:" + event.getChannelID());
    this.clientHandshakeManager.disconnected();
  }

  private void channelClosed(ChannelEvent event) {
    LOGGER.debug("channel closed:" + event.getChannelID());
    requestDisconnect();
  }

  private void channelReconnectionRejected() {
    LOGGER.debug("channel rejected");
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
