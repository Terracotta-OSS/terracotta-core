/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * dev-null implementation of a writable byte channel, you can specify the maximum number of bytes to write at once by
 * calling {@link MockWritableByteChannel#setMaxWriteCount(long)}.
 */
public class MockWritableByteChannel extends MockChannel implements WritableByteChannel {

  private long    maxWriteCount = Long.MAX_VALUE;

  public final synchronized int write(ByteBuffer src) throws IOException {
    checkOpen();
    if (src == null) { throw new IOException("null ByteBuffer passed in to write(ByteBuffer)"); }
    int writeCount = 0;
    while (src.hasRemaining() && writeCount < getMaxWriteCount()) {
      src.get();
      ++writeCount;
    }
    return writeCount;
  }

  synchronized final void setMaxWriteCount(long maxBytesToWriteAtOnce) {
    maxWriteCount = maxBytesToWriteAtOnce;
  }

  protected final synchronized long getMaxWriteCount() {
    return maxWriteCount;
  }

}
