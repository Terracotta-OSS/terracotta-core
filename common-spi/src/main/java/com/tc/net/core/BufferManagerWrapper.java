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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 */
public class BufferManagerWrapper implements SocketEndpoint {
  
  private final BufferManager base;

  public BufferManagerWrapper(BufferManager base) {
    this.base = base;
  }
  
  @Override
  public ResultType writeFrom(ByteBuffer[] ref) throws IOException {
    int sentOnWire = 0;
    int transfered = 0;
    for (ByteBuffer b : ref) {
      while (b.hasRemaining()) {
        int amt = base.forwardToWriteBuffer(b);
        if (amt == 0) {
          int sent = base.sendFromBuffer();
          if (sent == 0) {
            break;
          } else {
            sentOnWire += sent;
          }
        } else {
          transfered += amt;
        }
      }
      if (b.hasRemaining()) {
        break;
      }
    }
    while (sentOnWire < transfered) {
      sentOnWire += base.sendFromBuffer();
    }
    return transfered > 0 ? ResultType.SUCCESS : ResultType.ZERO;
  }
  
  @Override
  public ResultType readTo(ByteBuffer[] ref) throws IOException {
    int bytesRead = 0;
    for (ByteBuffer buf : ref) {
      if (buf.hasRemaining()) {
        final int read = base.forwardFromReadBuffer(buf);

        if (0 == read) {
          if (base.recvToBuffer() == 0) {
            break;
          }
        }

        bytesRead += read;

        if (buf.hasRemaining()) {
          // don't move on to the next buffer if we didn't fill the current one
          break;
        }
      }
    }
    
    return bytesRead > 0 ? ResultType.SUCCESS : ResultType.ZERO;
  }

  @Override
  public void close() throws IOException {
    base.close();
  }
}
