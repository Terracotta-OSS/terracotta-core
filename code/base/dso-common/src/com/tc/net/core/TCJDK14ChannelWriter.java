/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import java.nio.channels.GatheringByteChannel;

/**
 * Interface used by comms thread to request writing a channel. This interface makes it possible to slide a stack
 * of between the write calls and the socket. An example layer would be something that can deal with encrypt/decrypt of
 * the stream
 * 
 * @author teck
 */
interface TCJDK14ChannelWriter {
  void doWrite(GatheringByteChannel channel);
}