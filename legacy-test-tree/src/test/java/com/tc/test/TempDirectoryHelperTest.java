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

import java.io.File;

import junit.framework.TestCase;

/**
 * Unit test for {@link TempDirectoryHelper}.
 */
public class TempDirectoryHelperTest extends TestCase {

  private File                baseFile;
  private File                expectedDir;
  private TempDirectoryHelper helper;

  @Override
  public void setUp() throws Exception {

    String root = TestConfigObject.getInstance().tempDirectoryRoot() + File.separator + "temp-TempDirectoryHelperTest";
    this.baseFile = new File(root);
    if (!this.baseFile.exists()) assertTrue(this.baseFile.mkdirs());
    this.expectedDir = new File(this.baseFile, "TempDirectoryHelperTest");
    if (this.expectedDir.exists()) FileUtils.deleteDirectory(this.expectedDir);
    this.helper = new TempDirectoryHelper(getClass(), root, true);
  }

  public void testCreates() throws Exception {
    assertFalse(this.expectedDir.exists());

    File theDir = this.helper.getDirectory();
    assertEquals(this.expectedDir.getAbsolutePath(), theDir.getAbsolutePath());

    assertTrue(this.expectedDir.exists());
    assertTrue(this.expectedDir.isDirectory());
  }

  public void testCleans() throws Exception {
    File theFile = new File(this.expectedDir, "foo.txt");
    assertTrue(this.expectedDir.mkdirs());
    assertTrue(theFile.createNewFile());
    assertTrue(theFile.exists());

    this.helper.getDirectory();
    assertFalse(theFile.exists());
  }

  public void testDoesNotReclean() throws Exception {
    this.helper.getDirectory();

    File theFile = new File(this.expectedDir, "foo.txt");
    assertTrue(theFile.createNewFile());
    assertTrue(theFile.exists());

    this.helper.getDirectory();
    assertTrue(theFile.exists());
  }

  public void testGetFile() throws Exception {
    File theFile = new File(this.expectedDir, "foo.txt");
    assertTrue(this.expectedDir.mkdirs());
    assertTrue(theFile.createNewFile());
    assertTrue(theFile.exists());

    File otherFile = this.helper.getFile("bar.txt");
    assertFalse(theFile.exists());
    assertFalse(otherFile.exists());

    assertTrue(theFile.createNewFile());
    assertTrue(otherFile.createNewFile());

    File thirdFile = this.helper.getFile("baz.txt");
    assertTrue(theFile.exists());
    assertTrue(otherFile.exists());
    assertFalse(thirdFile.exists());
    assertTrue(thirdFile.createNewFile());
    assertTrue(thirdFile.exists());

    File theDir = this.helper.getDirectory();
    assertTrue(theFile.exists());
    assertTrue(otherFile.exists());
    assertEquals(theDir.getAbsolutePath(), theFile.getParentFile().getAbsolutePath());
  }

}