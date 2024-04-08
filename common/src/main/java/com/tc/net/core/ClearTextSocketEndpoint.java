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
import java.nio.channels.SocketChannel;

/**
 *
 */
public class ClearTextSocketEndpoint implements SocketEndpoint {
  
  private final SocketChannel socket;
  private volatile boolean open = true;

  public ClearTextSocketEndpoint(SocketChannel socket) {
    this.socket = socket;
  }

  @Override
  public ResultType writeFrom(ByteBuffer[] ref) throws IOException {
    if (!open) return ResultType.EOF;
    long amount = socket.write(ref);

    if (amount == 0) {
      return ResultType.ZERO;
    } else if (amount < 0) {
      return ResultType.EOF;
    } else {
      return ResultType.SUCCESS;
    }
  }

  @Override
  public ResultType readTo(ByteBuffer[] ref) throws IOException {
    if (!open) return ResultType.EOF;
    long amount = socket.read(ref);
    if (amount == 0) {
      return ResultType.ZERO;
    } else if (amount < 0) {
      return ResultType.EOF;
    } else {
      return ResultType.SUCCESS;
    }
  }

  @Override
  public void close() throws IOException {
    open = false;
  }
}
