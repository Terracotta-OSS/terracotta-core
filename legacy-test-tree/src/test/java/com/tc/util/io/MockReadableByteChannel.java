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
package com.tc.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * dev-zero implementation of a readable byte channel, you can specify the maximum number of bytes to read at once by
 * calling {@link MockReadableByteChannel#setMaxReadCount(long)}.
 */
public class MockReadableByteChannel extends MockChannel implements ReadableByteChannel {

  private long maxReadCount = Long.MAX_VALUE;

  @Override
  public final synchronized int read(ByteBuffer dst) throws IOException {
    checkOpen();
    dst.isReadOnly(); // NPE check
    int readCount = 0;
    while (dst.hasRemaining() && readCount < getMaxReadCount()) {
      dst.put((byte) 0x00);
      ++readCount;
    }
    return readCount;
  }

  synchronized final void setMaxReadCount(long maxBytesToReadAtOnce) {
    maxReadCount = maxBytesToReadAtOnce;
  }

  protected final synchronized long getMaxReadCount() {
    return maxReadCount;
  }

}
