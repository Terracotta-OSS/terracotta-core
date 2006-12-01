/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import java.nio.channels.ScatteringByteChannel;

/**
 * Interface used by comms thread to request reading from a channel. This interface makes it possible to slide a stack
 * of between the read calls and the socket. An example layer would be something that can deal with encrypt/decrypt of
 * the stream
 * 
 * @author teck
 */
interface TCJDK14ChannelReader {
  public void doRead(ScatteringByteChannel channel);
}