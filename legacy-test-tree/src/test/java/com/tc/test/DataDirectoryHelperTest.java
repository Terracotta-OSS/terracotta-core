/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;

import junit.framework.TestCase;

/**
 * Unit test for {@link DataDirectoryHelper}.
 */
public class DataDirectoryHelperTest extends TestCase {

  private File                baseFile;
  private File                expectedDir;
  private DataDirectoryHelper helper;

  public void setUp() throws Exception {
    String root = TestConfigObject.getInstance().tempDirectoryRoot() + File.separator + "temp-DataDirectoryHelperTest";

    this.baseFile = new File(root);
    this.expectedDir = new File(this.baseFile, ClassBasedDirectoryTreeTest.joinWithFileSeparator(new String[] { "com",
        "tc", "test", "DataDirectoryHelperTest" }));
    if (this.expectedDir.exists()) FileUtils.deleteDirectory(this.expectedDir);
    assertTrue(this.expectedDir.mkdirs());

    this.helper = new DataDirectoryHelper(getClass(), root);
  }

  public void testFailsIfNonexistent() throws Exception {
    assertTrue(this.expectedDir.delete());
    assertFalse(this.expectedDir.exists());

    try {
      this.helper.getDirectory();
      fail("Didn't get exception on getDirectory() with no directory there");
    } catch (FileNotFoundException fnfe) {
      // ok
    }
  }

  public void testGetDirectory() throws Exception {
    assertTrue(this.expectedDir.exists());

    File dataFile = new File(this.expectedDir, "foo.txt");
    assertTrue(dataFile.createNewFile());
    assertTrue(dataFile.exists());

    File theDirectory = this.helper.getDirectory();
    assertEquals(this.expectedDir.getAbsolutePath(), theDirectory.getAbsolutePath());
    assertTrue(theDirectory.exists());
    assertTrue(dataFile.exists());

    theDirectory = this.helper.getDirectory();
    assertEquals(this.expectedDir.getAbsolutePath(), theDirectory.getAbsolutePath());
    assertTrue(theDirectory.exists());
    assertTrue(dataFile.exists());
  }

  public void testGetFile() throws Exception {
    File theFile = new File(this.expectedDir, "foo.txt");
    assertTrue(theFile.createNewFile());
    assertTrue(theFile.exists());

    File fromHelper = this.helper.getFile("foo.txt");
    assertTrue(theFile.exists());
    assertEquals(theFile.getAbsolutePath(), fromHelper.getAbsolutePath());

    try {
      this.helper.getFile("nonexistent.txt");
      fail("Didn't get exception on get of nonexistent file");
    } catch (FileNotFoundException fnfe) {
      // ok
    }
  }

}