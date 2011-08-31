/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.io.TCFile;
import com.tc.io.TCFileChannel;
import com.tc.io.TCFileLock;
import com.tc.io.TCRandomFileAccess;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class AbstractStartupLock implements StartupLock {
  private static final TCLogger logger = TCLogging.getLogger(BlockingStartupLock.class);
  private final TCFile          location;
  protected TCFileLock          lock;
  protected TCFileChannel       channel;
  protected boolean             isBlocked;
  private final boolean         retry;

  public AbstractStartupLock(TCFile location, boolean retries) {
    this.location = location;
    this.retry = retries;
  }

  public synchronized void release() {
    try {
      if (lock != null) {
        try {
          this.lock.release();
          this.isBlocked = false;
        } catch (IOException e) {
          logger.error(e);
        }
      }

      if (channel != null) {
        try {
          channel.close();
        } catch (IOException e) {
          logger.error(e);
        }
      }
    } finally {
      lock = null;
      channel = null;
    }
  }

  public synchronized boolean canProceed(TCRandomFileAccess randomFileAccess) throws LocationNotCreatedException,
      FileNotCreatedException {
    // Get a file channel for the file

    TCFile tcFile = location.createNewTCFile(location, "startup.lck");

    ensureLocationExists();
    ensureFileExists(tcFile);

    try {
      channel = randomFileAccess.getChannel(tcFile, "rw");
    } catch (FileNotFoundException fnfe) {
      throw new TCAssertionError(fnfe);
    }

    while (true) {
      try {
        requestLock(tcFile);
        break;
      } catch (TCDataFileLockingException tdfle) {
        if (!retry) {
          throw tdfle;
        } else {
          logger.error(tdfle.getMessage() + ". Retrying");
        }
      }
    }

    return lock != null;
  }

  protected abstract void requestLock(TCFile tcFile);

  private void ensureFileExists(TCFile file) throws FileNotCreatedException {
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        throw new FileNotCreatedException("Could not create file for startup lock: " + file
                                          + ". Please ensure that this file can be created.");
      }
      Assert.eval(file.exists());
    }
  }

  private void ensureLocationExists() throws LocationNotCreatedException {
    if (!location.exists()) {
      try {
        location.forceMkdir();
      } catch (IOException e) {
        throw new LocationNotCreatedException("Could not create location for startup lock: " + location
                                              + ". Please ensure that this directory can be created. " + e.getMessage());
      }
    }
  }

}
