/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import java.io.File;
import java.io.IOException;

public interface TCFile {

  public TCFile createNewTCFile(TCFile location, String fileName);
 
  public boolean exists();
  
  public void forceMkdir() throws IOException;
  
  public File getFile();
  
  public boolean createNewFile() throws IOException;
 
}
