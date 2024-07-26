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

import com.tc.bytes.TCByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;

/**
 *
 */
public class CachingClearTextBufferManager extends ClearTextBufferManager {

  private final Queue<TCByteBuffer> retPool;
  private final TCByteBuffer send;
  private final TCByteBuffer recv;

  public CachingClearTextBufferManager(SocketChannel channel, TCByteBuffer send, TCByteBuffer recv, Queue<TCByteBuffer> returnpool) {
    super(channel, send.getNioBuffer(), recv.getNioBuffer());
    this.retPool = returnpool;
    this.send = send;
    this.recv = recv;
  }

  @Override
  public void close() {
    super.close();
  }

  @Override
  public void dispose() {
    recv.returnNioBuffer(getRecvBuffer());
    send.returnNioBuffer(getSendBuffer());
    super.dispose();
    retPool.offer(send.reInit());
    retPool.offer(recv.reInit());
  }
}
