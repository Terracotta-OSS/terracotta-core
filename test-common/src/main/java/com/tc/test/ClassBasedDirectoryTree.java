/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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

import org.apache.commons.io.FileUtils;

import com.tc.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Represents a tree of directories, based on class name. Used for the test data and directory trees.
 */
public class ClassBasedDirectoryTree {

  private final File root;

  public ClassBasedDirectoryTree(File root) throws FileNotFoundException {
    Assert.assertNotNull(root);
    if ((!root.exists()) || (!root.isDirectory())) throw new FileNotFoundException(
                                                                                   "Root '"
                                                                                                                                                                      + root
                                                                                                                                                                          .getAbsolutePath()
                                                                                                                                                                      + "' does not exist, or is not a directory..");
    this.root = root;
  }

  public File getDirectory(Class<?> theClass) throws IOException {
    String[] parts = theClass.getName().split("\\.");
    File destFile = buildFile(this.root, parts, 0);

    if (destFile.exists() && (!destFile.isDirectory())) throw new FileNotFoundException(
                                                                                        "'"
                                                                                                                                                                                + destFile
                                                                                                                                                                                + "' exists, but is not a directory.");
    return destFile;
  }

  public File getOrMakeDirectory(Class<?> theClass) throws IOException {
    File destFile = getDirectory(theClass);
    if (!destFile.exists()) FileUtils.forceMkdir(destFile);
    return destFile;
  }

  private File buildFile(File base, String[] parts, int startWhere) {
    if (startWhere >= parts.length) return base;
    else return buildFile(new File(base, parts[startWhere]), parts, startWhere + 1);
  }

}