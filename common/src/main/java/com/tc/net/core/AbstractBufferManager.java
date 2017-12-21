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

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

public abstract class AbstractBufferManager implements BufferManager {

  @Override
  public int forwardFromReadBuffer(ByteBuffer dest) {
    ByteBuffer recvBuffer = getRecvBuffer();
    recvBuffer.flip();
    int forwarded = forwardBuffer(recvBuffer, dest);
    recvBuffer.compact();
    return forwarded;
  }

  @Override
  public int forwardFromReadBuffer(GatheringByteChannel gbc) throws IOException {
    ByteBuffer recvBuffer = getRecvBuffer();
    recvBuffer.flip();
    int forwarded = gbc.write(recvBuffer);
    recvBuffer.compact();
    if (forwarded == -1) { throw new EOFException(); }
    return forwarded;
  }

  @Override
  public int forwardToWriteBuffer(ByteBuffer src) {
    ByteBuffer sendBuffer = getSendBuffer();
    return forwardBuffer(src, sendBuffer);
  }

  @Override
  public int forwardToWriteBuffer(ScatteringByteChannel sbc) throws IOException {
    ByteBuffer sendBuffer = getSendBuffer();
    int read = sbc.read(sendBuffer);
    if (read == -1) { throw new EOFException(); }
    return read;
  }

  protected abstract ByteBuffer getRecvBuffer();

  protected abstract ByteBuffer getSendBuffer();

  private static int forwardBuffer(ByteBuffer source, ByteBuffer dest) {
    int size = Math.min(dest.remaining(), source.remaining());
    if (size > 0) {
      ByteBuffer tmpBuf = source.duplicate();
      tmpBuf.limit(tmpBuf.position() + size);
      dest.put(tmpBuf);
      source.position(source.position() + size);
    }
    return size;
  }
}
