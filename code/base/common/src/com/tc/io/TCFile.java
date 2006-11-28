/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
