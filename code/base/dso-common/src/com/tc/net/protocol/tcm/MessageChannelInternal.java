/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportListener;

/**
 * The internal (comms-side) interface to the message channel. It acts like the bottom half of a NetworkLayer in that it
 * sends and receives messages -- but there's not a proper NetworkLayer above it to pass messages up to. It needs to be
 * a MessageTransportListener since in some stack configurations, it needs to respond to transport events
 * 
 * @author teck
 */
public interface MessageChannelInternal extends NetworkLayer, MessageChannel, MessageTransportListener {
  /**
   * Called when the transport's connection disrupted (OOO trying to restore)
   */
  public void notifyTransportDisrupted(MessageTransport transport);

  /**
   * Called when OOO successfully restore connection
   */
  public void notifyTransportRestored(MessageTransport transport);

}