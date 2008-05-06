/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;

public interface TCFileChannel {
  public TCFileLock lock() throws IOException, OverlappingFileLockException;
  
  public TCFileLock tryLock() throws IOException, OverlappingFileLockException;

  public void close() throws IOException;
}