/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.exception.ImplementMe;
import com.tc.io.TCFile;
import com.tc.io.TCFileChannel;
import com.tc.io.TCFileLock;
import com.tc.io.TCRandomFileAccess;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.io.File;
import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;

import junit.framework.TestCase;

public class StartupLockTest extends TestCase {

  private final int lockedAlreadyOnThisVM = 0;
  private final int lockCanBeAquired      = 1;

  public void testBasics() throws Throwable {
    TestTCRandomFileAccessImpl randomFileAccess = new TestTCRandomFileAccessImpl();

    boolean locationIsMakable = false;
    boolean fileIsmakable = false;
    TestTCFileImpl location = new TestTCFileImpl(locationIsMakable);
    location.setNewFileIsMakable(fileIsmakable);
    StartupLock startupLock = new StartupLock(location);
    try {
      startupLock.canProceed(randomFileAccess, true);
      fail("Expected LocationNotCreatedException. Not thrown.");
    } catch (LocationNotCreatedException se) {
      // ok
    }
    Assert.assertFalse(location.exists());

    locationIsMakable = true;
    fileIsmakable = false;
    location = new TestTCFileImpl(locationIsMakable);
    location.setNewFileIsMakable(fileIsmakable);
    startupLock = new StartupLock(location);
    try {
      startupLock.canProceed(randomFileAccess, true);
      fail("Expected FileNotCreatedException. Not thrown.");
    } catch (FileNotCreatedException se) {
      // ok
    }
    Assert.assertTrue(location.exists());

    randomFileAccess.setLockAvailability(lockedAlreadyOnThisVM);
    locationIsMakable = true;
    fileIsmakable = true;
    location = new TestTCFileImpl(locationIsMakable);
    location.setNewFileIsMakable(fileIsmakable);
    startupLock = new StartupLock(location);
    try {
      startupLock.canProceed(randomFileAccess, true);
      fail("Expected AssertionError for OverlappingFileLockException. Not thrown.");
    } catch (AssertionError se) {
      // ok
    }

    randomFileAccess.setLockAvailability(lockCanBeAquired);
    locationIsMakable = true;
    fileIsmakable = true;
    location = new TestTCFileImpl(locationIsMakable);
    location.setNewFileIsMakable(fileIsmakable);
    startupLock = new StartupLock(location);
    boolean result = startupLock.canProceed(randomFileAccess, true);
    Assert.assertTrue(location.exists());
    Assert.assertTrue(result);
  }

  private class TestTCRandomFileAccessImpl implements TCRandomFileAccess {
    private int lockAvailability;

    public void setLockAvailability(int lockAvailability) {
      this.lockAvailability = lockAvailability;
    }

    public TCFileChannel getChannel(TCFile tcFile, String mode) {
      return new TestTCFileChannelImpl(tcFile, mode, lockAvailability);
    }
  }

  private class TestTCFileChannelImpl implements TCFileChannel {

    private int lockAvailability;

    public TestTCFileChannelImpl(TCFile file, String mode, int lockAvailability) {
      this.lockAvailability = lockAvailability;
    }

    public TCFileLock lock() throws OverlappingFileLockException {
      if (lockAvailability == lockedAlreadyOnThisVM) { throw new OverlappingFileLockException(); }
      return new TestTCFileLockImpl();
    }

    public void close() {
      // method is not used in test
    }

    public TCFileLock tryLock() throws OverlappingFileLockException {
      throw new ImplementMe();
    }

  }

  private class TestTCFileLockImpl implements TCFileLock {

    public void release() {
      // method not used in test
    }

  }

  private class TestTCFileImpl implements TCFile {

    private boolean fileIsMakable;
    private boolean fileExists;
    private boolean newFileIsMakable;

    public TestTCFileImpl(boolean isMakable) {
      fileIsMakable = isMakable;
      fileExists = false;
    }

    public boolean exists() {
      return fileExists;
    }

    public void forceMkdir() throws IOException {
      if (!fileIsMakable) { throw new IOException("Could not create indicated directory."); }

      fileExists = true;
    }

    public File getFile() {
      return null;
    }

    public TCFile createNewTCFile(TCFile location, String fileName) {
      return new TestTCFileImpl(newFileIsMakable);
    }

    public boolean createNewFile() throws IOException {
      if (!fileIsMakable) { throw new IOException("Could not create indicated directory."); }

      fileExists = true;
      return fileExists;
    }

    public void setNewFileIsMakable(boolean val) {
      newFileIsMakable = val;
    }

    public String toString() {
      String s = "TestTCFileImpl:  ";
      if (fileIsMakable) {
        s += "file is makable, ";
      } else {
        s += "file is not makable, ";
      }
      if (fileExists) {
        s += "file exists.";
      } else {
        s += "file does not exist.";
      }
      return s;
    }
  }

}
