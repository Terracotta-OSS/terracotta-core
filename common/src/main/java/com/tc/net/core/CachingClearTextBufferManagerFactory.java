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
import com.tc.bytes.TCDirectByteBufferCache;
import com.tc.text.PrettyPrintable;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public class CachingClearTextBufferManagerFactory implements BufferManagerFactory, PrettyPrintable {
  private final TCDirectByteBufferCache buffers = new TCDirectByteBufferCache(ClearTextBufferManager.BUFFER_SIZE_KB);

  @Override
  public BufferManager createBufferManager(SocketChannel socketChannel, boolean client) {
    TCByteBuffer send = buffers.poll();
    TCByteBuffer recv = buffers.poll();
    
    return new CachingClearTextBufferManager(socketChannel, send, recv, buffers);
  }


  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("type", this.getClass().getName());
    state.put("pool.size", buffers.size());
    state.put("pool.referenced", buffers.referenced());
    return state;
  }
}
