/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.OverlappingFileLockException;

/**
 * Class for testing if it is safe to startup a process on a specified directory (i.e. will it corrupt a db)
 */
public class StartupLock {
  private static final TCLogger logger = TCLogging.getLogger(StartupLock.class);
  private final TCFile          location;
  private TCFileLock            lock;
  private TCFileChannel         channel;

  public StartupLock(TCFile location) {
    this.location = location;
  }

  public synchronized void release() {
    try {
      if (lock != null) {
        try {
          this.lock.release();
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

    try {
      if (block) {
        lock = channel.lock();
      } else {
        lock = channel.tryLock();
      }
    } catch (OverlappingFileLockException e) {
      // File is already locked in this thread or virtual machine
    } catch (IOException ioe) {
      reportDataFileLockingError("Unable to acquire file lock on '" + tcFile.getFile().getAbsolutePath()
          + "'.  Aborting Terracotta server startup.", null);
      throw new TCAssertionError(ioe);
    }

    Assert.eval(tcFile.exists());
    return lock != null;
  }

  private void reportDataFileLockingError(String message, Exception e) {
    StringBuffer errorMsg = new StringBuffer("\n");

    if (message != null) {
      errorMsg.append("ERROR: ").append(message).append("\n");
    }

    if (e != null) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      errorMsg.append(sw.toString());
    }

    System.err.println(errorMsg.toString());
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
            + ". Please ensure that this directory can be created.");
      }
    }
  }
}
