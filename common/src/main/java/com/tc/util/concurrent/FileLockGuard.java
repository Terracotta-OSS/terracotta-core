/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * This class allows a code section to be guarded by a file lock.
 * <p/>
 * The guarding is guaranteed both when the lock file is used within the
 * same JVM as well as when several JVMs are being used. This extends the
 * behavior of the standard JDK {@link FileLock} class, which only guards
 * when several JVMs are concurrently using the lock.
 */
public abstract class FileLockGuard {
  private final static TCLogger logger = CustomerLogging.getDSOGenericLogger();

  public static void guard(File lockFile, Guarded guarded) throws IOException, InnerException {
    if (logger.isDebugEnabled()) {
      logger.debug("Guarding through lock file '" + lockFile.getAbsolutePath() + "', guarded instance : "+guarded+".");
    }
    // synchronize on the interned string of the lock file for in-vm guarding
    synchronized (lockFile.getAbsolutePath().intern()) {
      RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
      FileChannel channel = raf.getChannel();
      // lock on a file for multi-vm guarding
      FileLock lock = channel.lock();
      try {
        guarded.execute();
      } finally {
        try {
          lock.release();
        } finally {
          raf.close();
        }
      }
    }
  }

  /**
   * Abstract class that needs to be extended by implementing the code
   * section that needs to be guarded in the {@link FileLockGuard.Guarded#execute()}
   * method.
   */
  public static abstract class Guarded {
    public abstract void execute() throws InnerException;
  }

  /**
   * This exception can be thrown within the guarded code section. The main
   * purpose is to be able to catch it outside the {@link FileLockGuard#guard(File, FileLockGuard.Guarded)}
   * method call so that the original inner exception can be retrieved and
   * re-thrown.
   */
  public static class InnerException extends Exception {
    private final Throwable innerException;

    public InnerException(Throwable innerException) {
      this.innerException = innerException;
    }

    public Throwable getInnerException() {
      return innerException;
    }
  }
}
