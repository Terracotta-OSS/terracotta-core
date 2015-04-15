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
import java.nio.channels.GatheringByteChannel;

/**
 * dev-null implementation of a gathering byte channel.
 */
public class MockGatheringByteChannel extends MockWritableByteChannel implements GatheringByteChannel {

  @Override
  public synchronized long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
    checkOpen();
    if (srcs == null) { throw new IOException("null ByteBuffer[] passed in to write(ByteBuffer[], int, int)"); }
    checkNull(srcs);
    throw new IOException("Not yet implemented");
  }

  @Override
  public synchronized long write(ByteBuffer[] srcs) throws IOException {
    checkOpen();
    if (srcs == null) { throw new IOException("null ByteBuffer[] passed in to write(ByteBuffer[])"); }
    checkNull(srcs);
    long bytesWritten = 0;
    for (int pos = 0; pos < srcs.length && bytesWritten < getMaxWriteCount(); ++pos) {
      ByteBuffer buffer = srcs[pos];
      while (buffer.hasRemaining() && bytesWritten < getMaxWriteCount()) {
        buffer.get();
        ++bytesWritten;
      }
    }
    return bytesWritten;
  }

  private void checkNull(ByteBuffer[] srcs) throws IOException {
    for (int pos = 0; pos < srcs.length; ++pos) {
      if (srcs[pos] == null) { throw new IOException("Null ByteBuffer at array position[" + pos + "]"); }
    }
  }
}
