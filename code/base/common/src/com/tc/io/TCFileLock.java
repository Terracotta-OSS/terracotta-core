/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.io;

import java.io.IOException;

public interface TCFileLock {
  
  public void release() throws IOException;
}
