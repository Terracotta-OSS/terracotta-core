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

import org.apache.commons.io.FileUtils;

import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Fetches temporary directories (and files) for use in tests. The directory is cleaned the first time it is fetched;
 * files may exist (or no), as appropriate.
 */
public class TempDirectoryHelper extends BaseDirectoryHelper {

  private final boolean cleanDir;

  public TempDirectoryHelper(Class theClass, boolean cleanDir) {
    this(theClass, TestConfigObject.getInstance().tempDirectoryRoot(), cleanDir);
  }

  public TempDirectoryHelper(Class theClass) {
    this(theClass, TestConfigObject.getInstance().tempDirectoryRoot(), true);
  }

  // For internal use and tests only.
  TempDirectoryHelper(Class theClass, String directoryPath, boolean cleanDir) {
    super(theClass, directoryPath);
    this.cleanDir = cleanDir;
  }

  @Override
  protected File fetchDirectory() throws IOException {
    File root = getRoot();
    if (!root.exists()) {
      root.mkdirs();
    }
    String shortClassName = getTargetClass().getName();
    String[] tokens = shortClassName.split("\\.");
    if (tokens.length > 1) {
      shortClassName = tokens[tokens.length - 1];
    }
    File directory = new File(root, shortClassName);
    if ((!directory.exists()) && (!directory.mkdirs())) {
      FileNotFoundException fnfe = new FileNotFoundException("Directory '" + directory.getAbsolutePath()
                                                             + "' can't be created.");
      throw fnfe;
    }

    if (cleanDir) {
      int count = 0;
      while (true) {
        count++;
        try {
          FileUtils.cleanDirectory(directory);
          break;
        } catch (Exception e) {
          System.err.println("Unable to clean up the directory - " + directory + "; Exception: " + e);
        }

        if (count > 10) {
          System.err.println("Skipping clean up for directory - " + directory);
          break;
        }

        ThreadUtil.reallySleep(2000);
      }
    }
    return directory;
  }
}