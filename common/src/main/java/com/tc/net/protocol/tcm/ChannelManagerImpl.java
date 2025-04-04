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
package com.tc.net.protocol.tcm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * provides the sessionIDs
 * 
 * @author steve
 */
class ChannelManagerImpl implements ChannelManager, ChannelEventListener, ServerMessageChannelFactory {
  private static final Logger logger              = LoggerFactory.getLogger(ChannelManager.class);
  private static final MessageChannelInternal[]        EMPTY_CHANNEL_ARARY = new MessageChannelInternal[] {};

  private final Map<ChannelID, MessageChannelInternal> channels;
  private final Predicate<MessageChannel>                                transportDisconnectRemovesChannel;
  private final ServerMessageChannelFactory            channelFactory;
  private final List<ChannelManagerEventListener>      eventListeners      = new CopyOnWriteArrayList<ChannelManagerEventListener>();

  public ChannelManagerImpl(Predicate<MessageChannel> transportDisconnectRemovesChannel, ServerMessageChannelFactory channelFactory) {
    this.channels = new HashMap<ChannelID, MessageChannelInternal>();
    this.transportDisconnectRemovesChannel = transportDisconnectRemovesChannel;
    this.channelFactory = channelFactory;
  }

  @Override
  public MessageChannelInternal createNewChannel(ChannelID id) {
    MessageChannelInternal channel = channelFactory.createNewChannel(id);
    synchronized (this) {
      if (id.isNull()) {
        throw new AssertionError();
      }
      channels.put(id, channel);
      channel.addListener(this);
    }
    return channel;
  }

  private void fireChannelCreatedEvent(MessageChannel channel) {
    for (ChannelManagerEventListener eventListener : eventListeners) {
      eventListener.channelCreated(channel);
    }
  }

  private void fireChannelRemovedEvent(MessageChannel channel) {
    for (ChannelManagerEventListener eventListener : eventListeners) {
      eventListener.channelRemoved(channel);
    }
  }

  @Override
  public synchronized MessageChannelInternal getChannel(ChannelID id) {
    return channels.get(id);
  }

  @Override
  public synchronized MessageChannelInternal[] getChannels() {
    return channels.values().toArray(EMPTY_CHANNEL_ARARY);
  }

  @Override
  public synchronized void closeAllChannels() {
    MessageChannelInternal[] channelsCopy = getChannels();
    for (MessageChannelInternal element : channelsCopy) {
      element.close();
    }
    Assert.assertEquals(0, channels.size());
  }

  @Override
  public synchronized Set<ChannelID> getAllChannelIDs() {
    return new HashSet<ChannelID>(channels.keySet());
  }

  @Override
  public synchronized boolean isValidID(ChannelID channelID) {
    if (channelID == null) { return false; }

    final MessageChannel channel = getChannel(channelID);

    if (channel == null) {
      logger.warn("no channel found for " + channelID);
      return false;
    }

    return true;
  }

  @Override
  public void notifyChannelEvent(ChannelEvent event) {
    MessageChannel channel = event.getChannel();

    if (ChannelEventType.CHANNEL_CLOSED_EVENT.matches(event)) {
      removeChannel(channel);
    } else if (ChannelEventType.TRANSPORT_DISCONNECTED_EVENT.matches(event)) {
      if (this.transportDisconnectRemovesChannel.test(channel)) {
        channel.close();
      }
    } else if (ChannelEventType.TRANSPORT_CONNECTED_EVENT.matches(event)) {
      fireChannelCreatedEvent(channel);
    }
  }

  private void removeChannel(MessageChannel channel) {
    boolean notfound;
    synchronized (this) {
      notfound = (channels.remove(channel.getChannelID()) == null);
    }
    if (notfound) {
      logger.warn("Remove non-exist channel:" + channel.getChannelID());
      return;
    }
    fireChannelRemovedEvent(channel);
  }

  @Override
  public synchronized void addEventListener(ChannelManagerEventListener listener) {
    if (listener == null) { throw new IllegalArgumentException("listener must be non-null"); }
    this.eventListeners.add(listener);
  }

}
