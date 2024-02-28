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
 * @author mscott2
 */
public class BufferManagerWrapper implements SocketEndpoint {
  
  private final BufferManager base;

  public BufferManagerWrapper(BufferManager base) {
    this.base = base;
  }
  
  public ResultType writeFrom(ByteBuffer[] ref) throws IOException {
    long total = 0;
    for (ByteBuffer b : ref) {
      while (b.hasRemaining()) {
        int amt = base.forwardToWriteBuffer(b);
        int sent = base.sendFromBuffer();
        while (sent != amt) {
          sent += base.sendFromBuffer();
        }
        total += sent;
      }
    }
    return ResultType.SUCCESS;
  }
  
  public ResultType readTo(ByteBuffer[] ref) throws IOException {
    long total = 0;
    for (ByteBuffer b : ref) {
      while (b.hasRemaining()) {
        int amt = base.forwardFromReadBuffer(b);
        if (amt == 0) {
          int read = base.recvToBuffer();
        }
        total += amt;
      }
    }
    return ResultType.SUCCESS;
  }

  @Override
  public void close() throws IOException {
    base.close();
  }
}
