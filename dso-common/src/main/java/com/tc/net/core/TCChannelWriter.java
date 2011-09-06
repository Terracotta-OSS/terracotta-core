/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import java.nio.channels.GatheringByteChannel;

/**
 * Interface used by {@link CoreNIOServices comms threads} to request writing a channel. This interface makes it
 * possible to slide a stack of between the write calls and the socket. An example layer would be something that can
 * deal with encrypt/decrypt of the stream
 * 
 * @author teck
 */
interface TCChannelWriter {
  int doWrite(GatheringByteChannel channel);
}