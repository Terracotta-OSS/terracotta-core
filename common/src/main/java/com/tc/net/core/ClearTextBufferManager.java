/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
  static final int      BUFFER_SIZE_KB = Integer.getInteger(BUFFER_SIZE, 16) * 1024;
  private final SocketChannel   channel;
  private final ByteBuffer      sendBuffer;
  private final ByteBuffer      recvBuffer;

  public ClearTextBufferManager(SocketChannel channel) {
    this(channel, ByteBuffer.allocate(BUFFER_SIZE_KB), ByteBuffer.allocate(BUFFER_SIZE_KB));
  }

  protected ClearTextBufferManager(SocketChannel channel, ByteBuffer send, ByteBuffer recv) {
    this.channel = channel;
    if (logger.isDebugEnabled()) {
      logger.debug(this.getClass().getName() + " " + BUFFER_SIZE + " " + BUFFER_SIZE_KB);
    }
    sendBuffer = send;
    recvBuffer = recv;
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
    int read = this.channel.read(getRecvBuffer());
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
