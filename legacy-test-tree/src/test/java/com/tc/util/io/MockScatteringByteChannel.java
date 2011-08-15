/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ScatteringByteChannel;

/**
 * dev-zero implementation of a readable channel.
 */
public class MockScatteringByteChannel extends MockReadableByteChannel implements ScatteringByteChannel {

  public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
    throw new IOException("Not yet implemented");
  }

  public long read(ByteBuffer[] dsts) throws IOException {
    checkOpen();
    if (dsts == null) { throw new IOException("null ByteBuffer[] passed in to read(ByteBuffer[])"); }
    checkNull(dsts);
    long bytesRead = 0;
    for (int pos = 0; pos < dsts.length && bytesRead < getMaxReadCount(); ++pos) {
      ByteBuffer buffer = dsts[pos];
      while (buffer.hasRemaining() && bytesRead < getMaxReadCount()) {
        buffer.put((byte) 0x00);
        ++bytesRead;
      }
    }
    return bytesRead;
  }

  private void checkNull(ByteBuffer[] srcs) throws IOException {
    for (int pos = 0; pos < srcs.length; ++pos) {
      if (srcs[pos] == null) { throw new IOException("Null ByteBuffer at array position[" + pos + "]"); }
    }
  }
}
