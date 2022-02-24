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
    retPool.offer(send.reInit());
    retPool.offer(recv.reInit());
  }
}
