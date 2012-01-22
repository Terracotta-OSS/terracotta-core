/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.io.TCFile;

import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;

/**
 * Class for testing if it is safe to startup a process on a specified directory (i.e. will it corrupt a db)
 */
public class BlockingStartupLock extends AbstractStartupLock implements StartupLock {

  public BlockingStartupLock(TCFile location, boolean retries) {
    super(location, retries);
  }

  @Override
  protected void requestLock(TCFile tcFile) {
    try {
      this.isBlocked = true;
      lock = channel.lock();
    } catch (OverlappingFileLockException e) {
      // File is already locked in this thread or virtual machine
      throw new AssertionError(e);
    } catch (IOException ioe) {
      throw new TCDataFileLockingException("Unable to acquire file lock on '" + tcFile.getFile().getAbsolutePath()
                                           + "'.  Aborting Terracotta server instance startup.");
    } finally {
      this.isBlocked = false;
    }
  }

  public boolean isBlocked() {
    return this.isBlocked;
  }
}
