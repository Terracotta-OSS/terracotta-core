/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportListener;

public interface NetworkStackHarnessFactory {

  /**
   * Creates server-side stack harnesses.
   * 
   * @param transportListeners An array of MessageTransportListeners that ought to be wired up to the transport (in
   *        addition to any that might be created by the stack harness)
   */
  NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory, MessageTransport transport,
                                          MessageTransportListener[] transportListeners);

  /**
   * Creates client-side stack harnesses.
   */
  NetworkStackHarness createClientHarness(MessageTransportFactory transportFactory, MessageChannelInternal channel,
                                          MessageTransportListener[] transportListeners);

}