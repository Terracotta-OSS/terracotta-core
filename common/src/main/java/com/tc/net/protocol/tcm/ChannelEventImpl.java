/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import java.util.Date;

public class ChannelEventImpl implements ChannelEvent {

  private final ChannelEventType type;
  private final MessageChannel   channel;
  private final Date             timestamp;

  public ChannelEventImpl(ChannelEventType type, MessageChannel channel) {
    this.type = type;
    this.channel = channel;
    this.timestamp = new Date();
  }

  @Override
  public String toString() {
    return getClass().getName() + "@" + System.identityHashCode(this) + "[type = " + this.type + ", timestamp = "
           + timestamp + ", channel  = " + channel + " remote node  : " + channel.getRemoteNodeID();

  }

  @Override
  public MessageChannel getChannel() {
    return channel;
  }

  @Override
  public ChannelID getChannelID() {
    return getChannel().getChannelID();
  }

  @Override
  public Date getTimestamp() {
    return timestamp;
  }

  @Override
  public ChannelEventType getType() {
    return type;
  }
}
