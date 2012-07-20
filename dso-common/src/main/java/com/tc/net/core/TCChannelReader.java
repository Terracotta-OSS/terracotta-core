/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core;

import java.nio.channels.ScatteringByteChannel;

/**
 * Interface used by {@link CoreNIOServices comms threads} to request reading from a channel. This interface makes it
 * possible to slide a stack of between the read calls and the socket. An example layer would be something that can deal
 * with encrypt/decrypt of the stream
 * 
 * @author teck
 */
interface TCChannelReader {
  public int doRead(ScatteringByteChannel channel);
}
