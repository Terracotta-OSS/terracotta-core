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
 * @author Ludovic Orban
 */
public interface BufferManager {
  /**
   * Called immediately after a call to recvToBuffer(). It will be called again if it returns a non-zero value.
   * It is called from the connection's read thread.
   * This method should write data into the supplied ByteBuffer.
   * @param dest the ByteBuffer into which data should be written
   * @return the number of bytes written
   */
  int forwardFromReadBuffer(ByteBuffer dest);

  /**
   * Called when there is data to write to the channel and the channel is accepting writes.
   * It is called from the connection's write thread.
   * This method should read data from the supplied ByteBuffer.
   * @param src the ByteBuffer from which data should be read
   * @return the number of bytes read
   */
  int forwardToWriteBuffer(ByteBuffer src);

  /**
   * Called immediately after a call to a forwardToWriteBuffer() method. It will be called repeatedly until the cumulative
   * total of bytes sent matches the number of bytes returned by the forwardToWriteBuffer() call.
   * This method should write data to the channel
   * @return the number of bytes sent
   * @throws IOException
   */
  int sendFromBuffer() throws IOException;

  /**
   * This method is called when the connection sees that data is available to read from the channel.
   * It is called from the connection's read thread.
   * This method should read from the channel. If it does not then it will be called again.
   * @return the number of bytes available to be read using a forwardFromReadBuffer() method - although this value is currently ignored
   * @throws IOException
   */
  int recvToBuffer() throws IOException;

  void close() throws IOException;
  
  default void dispose() {
    
  }
   
}
