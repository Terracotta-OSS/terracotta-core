/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io;

import java.io.IOException;
import java.nio.channels.FileLock;

public class TCFileLockImpl implements TCFileLock {
  
  private final FileLock lock;

  public TCFileLockImpl(FileLock lock) {
    this.lock = lock;
  }

  public void release() throws IOException {
    lock.release();
  }
}
