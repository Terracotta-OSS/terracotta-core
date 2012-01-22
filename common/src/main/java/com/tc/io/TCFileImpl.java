/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
