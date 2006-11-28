/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.io;

import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;

public interface TCFileChannel {
  public TCFileLock lock() throws IOException, OverlappingFileLockException;
  
  public TCFileLock tryLock() throws IOException, OverlappingFileLockException;

  public void close() throws IOException;
}