/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

/**
 * @author Ludovic Orban
 */
public interface BufferManager {
  int forwardFromReadBuffer(ByteBuffer dest);

  int forwardToWriteBuffer(ByteBuffer src);

  int sendFromBuffer() throws IOException;

  int recvToBuffer() throws IOException;

  void close() throws IOException;

  // These methods are used by the PipeSocket.
  int forwardFromReadBuffer(GatheringByteChannel gbc) throws IOException;

  int forwardToWriteBuffer(ScatteringByteChannel sbc) throws IOException;

}
