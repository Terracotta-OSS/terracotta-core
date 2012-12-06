/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.context;

import com.tc.async.api.EventContext;
import com.tc.net.protocol.tcm.MessageChannel;

public class RejoinContext implements EventContext {
  private final MessageChannel messageChannel;

  public RejoinContext(MessageChannel messageChannel) {
    this.messageChannel = messageChannel;
  }

  public MessageChannel getMessageChannel() {
    return messageChannel;
  }

}
