/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
import java.nio.channels.OverlappingFileLockException;

/**
 * Class for testing if it is safe to startup a process on a specified directory (i.e. will it corrupt a db)
 */
public class StartupLock {
  private static final TCLogger logger = TCLogging.getLogger(StartupLock.class);
  private final TCFile          location;
  private TCFileLock            lock;
  private TCFileChannel         channel;
  private volatile boolean      blocking;
  private final boolean         retries;

  public StartupLock(TCFile location) {
    this(location, false);
  }

  public StartupLock(TCFile location, boolean retries) {
    this.location = location;
    this.retries = retries;
  }

  public synchronized void release() {
    try {
      if (lock != null) {
        try {
          this.lock.release();
          blocking = false;
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

  public synchronized boolean canProceed(TCRandomFileAccess randomFileAccess, boolean block)
      throws LocationNotCreatedException, FileNotCreatedException {
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
        requestLock(tcFile, block);
        break;
      } catch (TCDataFileLockingException tdfle) {
        if (!retries) {
          throw tdfle;
        } else {
          logger.error(tdfle.getMessage() + ". Retrying");
        }
      }
    }

    return lock != null;
  }

  private void requestLock(TCFile tcFile, boolean block) {
    try {
      if (block) {
        blocking = true;
      }
      lock = channel.tryLock();
    } catch (OverlappingFileLockException e) {
      // File is already locked in this thread or virtual machine
      throw new AssertionError(e);
    } catch (IOException ioe) {
      throw new TCDataFileLockingException("Unable to acquire file lock on '" + tcFile.getFile().getAbsolutePath()
                                           + "'.  Aborting Terracotta server instance startup.");
    } finally {
      blocking = false;
    }

  }

  public boolean isBlocking() {
    return this.blocking;
  }

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
