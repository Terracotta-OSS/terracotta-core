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
