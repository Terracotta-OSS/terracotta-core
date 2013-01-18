/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.context;

import com.tc.async.api.EventContext;
import com.tc.net.protocol.tcm.ClientMessageChannel;

public class RejoinContext implements EventContext {
  private final ClientMessageChannel messageChannel;

  public RejoinContext(ClientMessageChannel messageChannel) {
    this.messageChannel = messageChannel;
  }

  public ClientMessageChannel getMessageChannel() {
    return messageChannel;
  }

}
