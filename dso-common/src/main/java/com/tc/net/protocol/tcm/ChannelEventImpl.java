/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  public String toString() {
    return getClass().getName() + "@" + System.identityHashCode(this) + "[type = " + this.type + ", timestamp = "
           + timestamp + ", channel  = " + channel + " remote node  : " + channel.getRemoteNodeID();

  }

  public MessageChannel getChannel() {
    return channel;
  }

  public ChannelID getChannelID() {
    return getChannel().getChannelID();
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public ChannelEventType getType() {
    return type;
  }
}