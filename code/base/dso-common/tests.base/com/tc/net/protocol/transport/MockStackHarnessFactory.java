/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;

public class MockStackHarnessFactory implements NetworkStackHarnessFactory {
  public NetworkStackHarness harness;

  public NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory,
                                                 MessageTransport transport,
                                                 MessageTransportListener[] transportListeners) {
    return harness;
  }

  public NetworkStackHarness clientClientHarness(MessageTransportFactory transportFactory,
                                                 MessageChannelInternal channel,
                                                 MessageTransportListener[] transportListeners) {
    throw new ImplementMe();
  }

}