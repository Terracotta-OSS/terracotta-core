/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
