/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author Ludovic Orban
 */
public class ClearTextBufferManager extends AbstractBufferManager {
  private static final Logger   logger         = LoggerFactory.getLogger(ClearTextBufferManager.class);
  private static final String   BUFFER_SIZE    = "clear.text.buffer.size";
  private static final int      BUFFER_SIZE_KB = Integer.getInteger(BUFFER_SIZE, 16) * 1024;
  private final SocketChannel   channel;
  private final ByteBuffer      sendBuffer     = ByteBuffer.allocate(BUFFER_SIZE_KB);
  private final ByteBuffer      recvBuffer     = ByteBuffer.allocate(BUFFER_SIZE_KB);

  public ClearTextBufferManager(SocketChannel channel) {
    this.channel = channel;
    if (logger.isDebugEnabled()) {
      logger.debug(this.getClass().getName() + " " + BUFFER_SIZE + " " + BUFFER_SIZE_KB);
    }
  }

  @Override
  public int sendFromBuffer() throws IOException {
    sendBuffer.flip();
    int written = this.channel.write(sendBuffer);
    sendBuffer.compact();
    if (written == -1) { throw new EOFException(); }
    return written;
  }

  @Override
  public int recvToBuffer() throws IOException {
    int read = this.channel.read(recvBuffer);
    if (read == -1) { throw new EOFException(); }
    return read;
  }

  @Override
  public void close() {
    //
  }

  @Override
  protected ByteBuffer getRecvBuffer() {
    return recvBuffer;
  }

  @Override
  protected ByteBuffer getSendBuffer() {
    return sendBuffer;
  }
}
