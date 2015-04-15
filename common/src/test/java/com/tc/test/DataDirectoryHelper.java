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

  @Override
  protected File fetchDirectory() throws IOException {
    ClassBasedDirectoryTree tree = new ClassBasedDirectoryTree(getRoot());
    File theDirectory = tree.getDirectory(getTargetClass());
    if (!theDirectory.exists()) throw new FileNotFoundException("No data directory '" + theDirectory.getAbsolutePath()
                                                                + "' exists.");
    return theDirectory;
  }

  @Override
  public File getFile(String path) throws IOException {
    File theFile = super.getFile(path);
    if (!theFile.exists()) throw new FileNotFoundException("No file '" + theFile.getAbsolutePath() + "' exists.");
    return theFile;
  }
}