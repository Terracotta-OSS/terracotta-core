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

  public File getDirectory(Class theClass) throws IOException {
    String[] parts = theClass.getName().split("\\.");
    File destFile = buildFile(this.root, parts, 0);

    if (destFile.exists() && (!destFile.isDirectory())) throw new FileNotFoundException(
                                                                                        "'"
                                                                                                                                                                                + destFile
                                                                                                                                                                                + "' exists, but is not a directory.");
    return destFile;
  }

  public File getOrMakeDirectory(Class theClass) throws IOException {
    File destFile = getDirectory(theClass);
    if (!destFile.exists()) FileUtils.forceMkdir(destFile);
    return destFile;
  }

  private File buildFile(File base, String[] parts, int startWhere) {
    if (startWhere >= parts.length) return base;
    else return buildFile(new File(base, parts[startWhere]), parts, startWhere + 1);
  }

}