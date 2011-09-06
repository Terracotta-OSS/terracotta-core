/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class TCFileChannelImpl implements TCFileChannel {

  private final FileChannel channel;

  public TCFileChannelImpl(FileChannel channel) {
    this.channel = channel;
  }

  public TCFileLock lock() throws IOException, OverlappingFileLockException {
    return new TCFileLockImpl(channel.lock());
  }

  public void close() throws IOException {
    channel.close();
  }

  public TCFileLock tryLock() throws IOException, OverlappingFileLockException {
    FileLock lock = channel.tryLock();
    if (lock != null) { return new TCFileLockImpl(lock); }
    return null;
  }

}
