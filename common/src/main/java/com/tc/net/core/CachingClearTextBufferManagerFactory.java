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
import com.tc.bytes.TCDirectByteBufferCache;
import com.tc.text.PrettyPrintable;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ludovic Orban
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
