/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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