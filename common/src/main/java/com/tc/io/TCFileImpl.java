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

  @Override
  public boolean exists() {
    return pathToFile.exists();
  }

  @Override
  public void forceMkdir() throws IOException {
    FileUtils.forceMkdir(pathToFile);
  }

  @Override
  public boolean createNewFile() throws IOException {
    return pathToFile.createNewFile();
  }

  @Override
  public File getFile() {
    return pathToFile;
  }

  @Override
  public TCFile createNewTCFile(TCFile location, String fileName) {
    return new TCFileImpl(location, fileName);
  }

  @Override
  public String toString() {
    return pathToFile.toString();
  }
}
