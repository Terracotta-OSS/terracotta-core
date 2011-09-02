/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.io;

import com.tc.test.TCTestCase;
import com.tc.test.TempDirectoryHelper;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.io.TCFileUtils;
import com.tc.util.io.TCFileUtils.EnsureWritableDirReporter;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class EnsureWritableDirTest extends TCTestCase {

  private final Random random = new Random();

  private File createTmpDir() throws IOException {
    File tmp_dir_parent = new TempDirectoryHelper(getClass(), false).getDirectory();
    File tmp_dir;
    synchronized (random) {
      tmp_dir = new File(tmp_dir_parent, "statisticsstore-" + random.nextInt() + "-" + System.currentTimeMillis());
    }
    tmp_dir.mkdirs();
    return tmp_dir;
  }

  /**
   * Test: ensureWritableDir(null, ...) should throw NPE
   */
  public void testNullDir() throws Exception {
    boolean caughtNPE = false;
    try {
      TCFileUtils.ensureWritableDir(null, new EnsureWritableDirReporter() {
        public void reportFailedCreate(File dir, Exception e) {
          fail("null directory should NPE");
        }

        public void reportReadOnly(File dir, Exception e) {
          fail("null directory should NPE");
        }
      });
    } catch (NullPointerException npe) {
      // expected
      caughtNPE = true;
    }
    assertTrue(caughtNPE);
  }

  /**
   * Test: ensureWritableDir() of an existing, writable dir should succeed
   */
  public void testNormalDir() throws Exception {
    File tmpDir = createTmpDir();
    boolean result = TCFileUtils.ensureWritableDir(tmpDir, new EnsureWritableDirReporter() {
      public void reportFailedCreate(File dir, Exception e) {
        fail();
      }

      public void reportReadOnly(File dir, Exception e) {
        fail();
      }
    });
    assertTrue(result);
  }

  /**
   * Test: ensureWritableDir() of nonexisting dir that can be created should succeed
   */
  public void testNonExistentDir() throws Exception {
    File tmpDir = createTmpDir();
    File childDir = new File(tmpDir, "child");
    boolean result = TCFileUtils.ensureWritableDir(childDir, new EnsureWritableDirReporter() {
      public void reportFailedCreate(File dir, Exception e) {
        fail();
      }

      public void reportReadOnly(File dir, Exception e) {
        fail();
      }
    });
    assertTrue(result);
  }

  /**
   * Test: ensureWritableDir() of a read-only dir should fail and call appropriate reporter function. Note that creating
   * a read-only dir in Java is fraught, because there is no way (before Java 6) of setting it back to writable. So
   * we're stuck with trying to delete a read-only dir, at the end of the test. Whether this works is system-dependent.
   * This test may not be able to run on all platforms.
   */
  public void testReadOnlyDir() throws Exception {
    File tmpDir = createTmpDir();
    int count = 0;
    while (!tmpDir.setReadOnly()) {
      ThreadUtil.reallySleep(2000);
      if (count++ > 5) {
        System.err.println("XXX Couldn't make " + tmpDir.getAbsolutePath() + " redable.");
        return;
      }
    }

    if (tmpDir.canWrite()) {
      System.err.println("XXX " + tmpDir.getAbsolutePath()
                         + " is writable even after read only set. Read test comments.");
      return;
    }

    final int calls[] = new int[] { 0 };
    boolean result = TCFileUtils.ensureWritableDir(tmpDir, new EnsureWritableDirReporter() {
      public void reportFailedCreate(File dir, Exception e) {
        fail();
      }

      public void reportReadOnly(File dir, Exception e) {
        ++calls[0];
      }
    });
    assertFalse(result);
    assertEquals(1, calls[0]);
  }

  /**
   * Test: ensureWritableDir() of non-existent dir whose parent is read-only should fail and call appropriate reporter
   * function. As with {@link #testReadOnlyDir()}, this test is awkward because there's no way to make the directory be
   * writable again after making it read-only.
   */
  public void testReadOnlyParent() throws Exception {
    // On Windows, the read-only attribute of a directory is ignored except when deleting
    // the directory itself, so this test is inapplicable.
    if (Os.isWindows()) { return; }
    File tmpDir = createTmpDir();
    tmpDir.setReadOnly();
    File child = new File(tmpDir, "child");
    final int calls[] = new int[] { 0 };
    boolean result = TCFileUtils.ensureWritableDir(child, new EnsureWritableDirReporter() {
      public void reportFailedCreate(File dir, Exception e) {
        ++calls[0];
      }

      public void reportReadOnly(File dir, Exception e) {
        fail();
      }
    });
    assertFalse(result);
    assertEquals(1, calls[0]);
  }

  /**
   * Test: ensureWritableDir() of a non-directory file should fail and call appropriate reporter function.
   */
  public void testNonDirFile() throws Exception {
    File tmpDir = createTmpDir();
    File child = new File(tmpDir, "child.txt");
    assertTrue(child.createNewFile());
    final int calls[] = new int[] { 0 };
    boolean result = TCFileUtils.ensureWritableDir(child, new EnsureWritableDirReporter() {
      public void reportFailedCreate(File dir, Exception e) {
        ++calls[0];
      }

      public void reportReadOnly(File dir, Exception e) {
        fail();
      }
    });
    assertFalse(result);
    assertEquals(1, calls[0]);
  }
}
