/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A helper that fetches data directories and files, for use in tests. These directories and files must exist.
 */
public class DataDirectoryHelper extends BaseDirectoryHelper {

  public DataDirectoryHelper(Class theClass) {
    this(theClass, TestConfigObject.getInstance().dataDirectoryRoot());
  }

  // For internal use and tests only.
  DataDirectoryHelper(Class theClass, String directoryPath) {
    super(theClass, directoryPath);
  }

  protected File fetchDirectory() throws IOException {
    ClassBasedDirectoryTree tree = new ClassBasedDirectoryTree(getRoot());
    File theDirectory = tree.getDirectory(getTargetClass());
    if (!theDirectory.exists()) throw new FileNotFoundException("No data directory '" + theDirectory.getAbsolutePath()
                                                                + "' exists.");
    return theDirectory;
  }

  public File getFile(String path) throws IOException {
    File theFile = super.getFile(path);
    if (!theFile.exists()) throw new FileNotFoundException("No file '" + theFile.getAbsolutePath() + "' exists.");
    return theFile;
  }
}