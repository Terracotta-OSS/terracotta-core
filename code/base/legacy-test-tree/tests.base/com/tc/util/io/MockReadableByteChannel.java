/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
