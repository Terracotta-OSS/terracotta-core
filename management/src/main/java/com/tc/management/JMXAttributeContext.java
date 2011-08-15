/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management;

import com.tc.async.api.EventContext;
import com.tc.net.protocol.tcm.MessageChannel;

import javax.management.remote.message.Message;

public class JMXAttributeContext implements EventContext {

  private final MessageChannel channel;
  private final Message        outboundMessage;

  public JMXAttributeContext(MessageChannel channel, Message outboundMessage) {
    this.channel = channel;
    this.outboundMessage = outboundMessage;
  }

  public MessageChannel getChannel() {
    return channel;
  }

  public Message getOutboundMessage() {
    return outboundMessage;
  }
}
