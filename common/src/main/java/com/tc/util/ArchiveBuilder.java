/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.io.File;
import java.io.IOException;

public interface ArchiveBuilder {

  public void putTraverseDirectory(File dir, String dirName) throws IOException;
  
  public void putDirEntry(String file) throws IOException;

  public void putEntry(String file, byte[] bytes) throws IOException;
  
  public void finish() throws IOException;
  
  public byte[] readFile(File file) throws IOException;
}
