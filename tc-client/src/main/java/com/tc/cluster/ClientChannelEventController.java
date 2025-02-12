/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import static com.tc.net.protocol.tcm.ChannelEventType.TRANSPORT_RECONNECTION_REJECTED_EVENT;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.util.CallStackTrace;


public class ClientChannelEventController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientChannelEventController.class);

  private final ClientHandshakeManager clientHandshakeManager;
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
    clientHandshakeManager.shutdown();
  }

  private void channelReconnectionRejected() {
    LOGGER.debug("channel rejected");
    clientHandshakeManager.fireNodeError();
    clientHandshakeManager.shutdown();
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
        case TRANSPORT_CLOSED_EVENT:
          controller.channelClosed(event);
          break;
        default:
          LOGGER.warn("Ignoring unexpected channel event " + event.getType() + " for channel " + eventChannelId);
          break;
      }
    }

  }

}
