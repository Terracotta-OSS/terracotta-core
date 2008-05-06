/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import com.tc.util.Assert;

import java.io.File;
import java.io.IOException;

/**
 * A helper that provides directories to tests. This is the base of {@link DataDirectoryHelper}and
 * {@link TempDirectoryHelper}.
 */
public abstract class BaseDirectoryHelper {

  private final Class theClass;
  private File        directoryPath;
  private File        theDirectory;

  protected BaseDirectoryHelper(Class theClass, String directoryPath) {
    Assert.assertNotNull(theClass);
    this.theClass = theClass;
    this.directoryPath = new File(directoryPath).getAbsoluteFile();
  }

  public synchronized File getDirectory() throws IOException {
    if (this.theDirectory == null) {
      this.theDirectory = fetchDirectory();
    }

    return this.theDirectory;
  }

  public File getFile(String path) throws IOException {
    return new File(getDirectory(), path);
  }

  protected abstract File fetchDirectory() throws IOException;

  protected final File getRoot() {
    return this.directoryPath;
  }

  protected final Class getTargetClass() {
    return this.theClass;
  }

}