/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

/**
 * @author Ludovic Orban
 */
public interface BufferManager {
  int forwardFromReadBuffer(ByteBuffer dest);

  int forwardToWriteBuffer(ByteBuffer src);

  int sendFromBuffer() throws IOException;

  int recvToBuffer() throws IOException;

  void close() throws IOException;

  // These methods are used by the PipeSocket.
  int forwardFromReadBuffer(GatheringByteChannel gbc) throws IOException;

  int forwardToWriteBuffer(ScatteringByteChannel sbc) throws IOException;
}
