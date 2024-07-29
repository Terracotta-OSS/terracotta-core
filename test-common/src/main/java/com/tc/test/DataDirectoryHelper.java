/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A helper that fetches data directories and files, for use in tests. These directories and files must exist.
 */
public class DataDirectoryHelper extends BaseDirectoryHelper {

  public DataDirectoryHelper(Class<?> theClass) {
    this(theClass, TestConfigObject.getInstance().dataDirectoryRoot());
  }

  // For internal use and tests only.
  DataDirectoryHelper(Class<?> theClass, String directoryPath) {
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