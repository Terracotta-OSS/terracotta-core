/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class TCFileImpl implements TCFile {
  private File pathToFile;
  
  public TCFileImpl(File pathToFile) {
    this.pathToFile = pathToFile;
  }
  
  public TCFileImpl(TCFile location, String fileName) {
    pathToFile = new File(location.getFile(), fileName);
  }

  public boolean exists() {
    return pathToFile.exists();
  }

  public void forceMkdir() throws IOException {
    FileUtils.forceMkdir(pathToFile);
  }

  public boolean createNewFile() throws IOException {
    return pathToFile.createNewFile();
  }

  public File getFile() {
    return pathToFile;
  }

  public TCFile createNewTCFile(TCFile location, String fileName) {
    return new TCFileImpl(location, fileName);
  }

  public String toString() {
    return pathToFile.toString();
  }
}
