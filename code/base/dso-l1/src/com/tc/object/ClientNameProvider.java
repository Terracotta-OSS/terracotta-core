/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeNameProvider;
import com.tc.object.net.DSOClientMessageChannel;

public class ClientNameProvider implements NodeNameProvider {

  private final DSOClientMessageChannel channel;

  public ClientNameProvider(DSOClientMessageChannel channel) {
    this.channel = channel;
  }

  public String getNodeName() {
    return this.channel.channel().getLocalNodeID().toString() + " "
           + this.channel.channel().getLocalAddress().getCanonicalStringForm();
  }

}
