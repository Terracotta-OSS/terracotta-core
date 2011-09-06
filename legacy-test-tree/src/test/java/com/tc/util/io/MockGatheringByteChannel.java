/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;

/**
 * dev-null implementation of a gathering byte channel.
 */
public class MockGatheringByteChannel extends MockWritableByteChannel implements GatheringByteChannel {

  public synchronized long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
    checkOpen();
    if (srcs == null) { throw new IOException("null ByteBuffer[] passed in to write(ByteBuffer[], int, int)"); }
    checkNull(srcs);
    throw new IOException("Not yet implemented");
  }

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
