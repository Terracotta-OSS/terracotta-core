/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

abstract class AbstractNetworkStackHarness implements NetworkStackHarness {
  protected MessageTransport                transport;
  protected MessageChannelInternal          channel;
  private final ServerMessageChannelFactory channelFactory;
  private final MessageTransportFactory     transportFactory;
  private final boolean                     isClientStack;
  private final SetOnceFlag                 finalized = new SetOnceFlag();

  AbstractNetworkStackHarness(ServerMessageChannelFactory channelFactory, MessageTransport transport) {
    this.channelFactory = channelFactory;
    this.transportFactory = null;
    this.transport = transport;
    this.isClientStack = false;
  }

  AbstractNetworkStackHarness(MessageTransportFactory transportFactory, MessageChannelInternal channel) {
    this.transportFactory = transportFactory;
    this.channelFactory = null;
    this.channel = channel;
    this.isClientStack = true;
  }

  /**
   * Connects a new transport to an existing stack (server-side).
   */
  public MessageTransport attachNewConnection(TCConnection connection) {
    Assert.eval("Attempt to connect a transport to a stack that hasn't been finalized.", finalized.isSet());
    this.transport.attachNewConnection(connection);
    return this.transport;
  }

  /**
   * Creates and connects a new stack.
   */
  public void finalizeStack() {
    if (finalized.attemptSet()) {
      if (isClientStack) {
        Assert.assertNotNull(this.channel);
        Assert.assertNotNull(this.transportFactory);
        this.transport = transportFactory.createNewTransport();
      } else {
        Assert.assertNotNull(this.transport);
        Assert.assertNotNull(this.channelFactory);
        this.channel = channelFactory.createNewChannel(new ChannelID(this.transport.getConnectionId().getChannelID()));
      }
      createIntermediateLayers();
      connectStack();
    } else {
      throw Assert.failure("Attempt to finalize an already finalized stack");
    }
  }

  protected abstract void createIntermediateLayers();

  protected abstract void connectStack();
}
