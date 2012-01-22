/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io;

import java.io.IOException;

public interface TCFileLock {
  
  public void release() throws IOException;
}
