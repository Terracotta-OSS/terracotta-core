/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.management.JMXAttributeContext;
import com.tc.net.protocol.tcm.MessageChannel;

import java.io.IOException;

import javax.management.remote.message.Message;

public class TunnelingMessageConnectionWrapper extends TunnelingMessageConnection {
  private final RemoteJMXAttributeProcessor jmAttributeProcessor = new RemoteJMXAttributeProcessor(); 

  public TunnelingMessageConnectionWrapper(MessageChannel channel, boolean isJmxConnectionServer) {
    super(channel, isJmxConnectionServer);
  }

  @Override
  public void writeMessage(Message outboundMessage) throws IOException {
    if (closed.isSet()) { throw new IOException("connection closed"); }
    
    JMXAttributeContext attributeContext = new JMXAttributeContext(this.channel, outboundMessage);
    jmAttributeProcessor.add(attributeContext);
  }
}
