/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Unit test for {@link ClassBasedDirectoryTree}.
 */
public class ClassBasedDirectoryTreeTest extends TCTestCase {

  private File                    root;
  private ClassBasedDirectoryTree tree;

  public void setUp() throws Exception {
    this.root = new File("ClassBasedDirectoryTreeTest");

    if (this.root.exists()) FileUtils.deleteDirectory(this.root);
    FileUtils.forceMkdir(this.root);
    this.tree = new ClassBasedDirectoryTree(root);
  }

  public void testConstructor() throws Exception {
    try {
      new ClassBasedDirectoryTree(null);
      fail("Didn't get NPE on null root");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new ClassBasedDirectoryTree(new File("thisdirectoryshouldneverexist"));
      fail("Didn't get exception on nonexistent root");
    } catch (FileNotFoundException fnfe) {
      // ok
    }

    File tempFile = new File("tempfileforcbdtt");
    assertTrue(tempFile.createNewFile());
    tempFile.deleteOnExit();

    try {
      new ClassBasedDirectoryTree(tempFile);
      fail("Didn't get exception on file root");
    } catch (FileNotFoundException fnfe) {
      // ok
    }

    assertTrue(tempFile.delete());
  }

  public void testGetNonexistentDirectory() throws Exception {
    File expectedFile = new File(this.root.getAbsolutePath()
                                 + File.separator
                                 + joinWithFileSeparator(new String[] { "com", "tc", "test",
                                     "ClassBasedDirectoryTreeTest" }));
    File theTest = this.tree.getDirectory(getClass());

    assertEquals(expectedFile.getAbsolutePath(), theTest.getAbsolutePath());
    assertFalse(theTest.exists());
    assertFalse(new File(this.root, "com").exists()); // make sure it didn't create any of the path at all
  }

  public void testGetExtantDirectory() throws Exception {
    File expectedFile = new File(this.root.getAbsolutePath()
                                 + File.separator
                                 + joinWithFileSeparator(new String[] { "com", "tc", "test",
                                     "ClassBasedDirectoryTreeTest" }));
    assertTrue(expectedFile.mkdirs());
    File otherFile = new File(expectedFile, "test.txt");
    assertTrue(otherFile.createNewFile());

    File theTest = this.tree.getDirectory(getClass());

    assertEquals(expectedFile.getAbsolutePath(), theTest.getAbsolutePath());
    assertTrue(expectedFile.exists() && expectedFile.isDirectory());
    assertTrue(otherFile.exists()); // make sure it didn't clean the directory
  }

  public void testGetOrMakeExtantDirectory() throws Exception {
    File expectedFile = new File(this.root.getAbsolutePath()
                                 + File.separator
                                 + joinWithFileSeparator(new String[] { "com", "tc", "test",
                                     "ClassBasedDirectoryTreeTest" }));
    assertTrue(expectedFile.mkdirs());
    File otherFile = new File(expectedFile, "test.txt");
    assertTrue(otherFile.createNewFile());

    File theTest = this.tree.getOrMakeDirectory(getClass());

    assertEquals(expectedFile.getAbsolutePath(), theTest.getAbsolutePath());
    assertTrue(expectedFile.exists() && expectedFile.isDirectory());
    assertTrue(otherFile.exists()); // make sure it didn't clean the directory
  }

  public void testFileInWay() throws Exception {
    File expectedFile = new File(this.root.getAbsolutePath()
                                 + File.separator
                                 + joinWithFileSeparator(new String[] { "com", "tc", "test",
                                     "ClassBasedDirectoryTreeTest" }));
    assertTrue(expectedFile.getParentFile().mkdirs());
    assertTrue(expectedFile.createNewFile());

    try {
      File theTest = this.tree.getDirectory(getClass());
      fail("Didn't get exception with file in the way");
      theTest.equals(null);
    } catch (IOException ioe) {
      // ok
    }

    try {
      this.tree.getOrMakeDirectory(getClass());
      fail("Didn't get exception with file in the way");
    } catch (IOException ioe) {
      // ok
    }
  }

  public void testFailsIfFileInWayOfPath() throws Exception {
    File expectedFile = new File(this.root.getAbsolutePath()
                                 + File.separator
                                 + joinWithFileSeparator(new String[] { "com", "tc", "test",
                                     "ClassBasedDirectoryTreeTest" }));
    assertTrue(expectedFile.getParentFile().getParentFile().mkdirs());
    assertTrue(expectedFile.getParentFile().createNewFile());

    File theTest = this.tree.getDirectory(getClass());
    assertEquals(expectedFile.getAbsolutePath(), theTest.getAbsolutePath());
    assertFalse(theTest.exists());

    try {
      this.tree.getOrMakeDirectory(getClass());
      fail("Didn't get exception with file in the way higher up the path");
    } catch (IOException ioe) {
      // ok
    }
  }

  static String joinWithFileSeparator(String[] s) {
    StringBuffer out = new StringBuffer();
    for (int i = 0; i < s.length; ++i) {
      if (i > 0) out.append(File.separator);
      out.append(s[i]);
    }

    return out.toString();
  }

  protected void tearDown() throws Exception {
    if (this.root.exists()) FileUtils.deleteDirectory(this.root);
  }

}