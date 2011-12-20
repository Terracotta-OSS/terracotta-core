/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.locks.ThreadID;
import com.tc.util.Conversion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ThreadDumpUtil {

  public static final String            ZIP_BUFFER_NAME         = "threadDumps.zip";
  private static final short            ZIP_BUFFER_INITIAL_SIZE = 10 * 1024;

  protected static final TCLogger       logger                  = TCLogging.getLogger(ThreadDumpUtil.class);
  protected static final ThreadMXBean   threadMXBean            = ManagementFactory.getThreadMXBean();
  protected static volatile ThreadGroup rootThreadGroup;

  public static byte[] getCompressedThreadDump() {
    return getCompressedThreadDump(new NullLockInfoByThreadIDImpl(), new NullThreadIDMapImpl());
  }

  public static byte[] getCompressedThreadDump(LockInfoByThreadID lockInfo, ThreadIDMap threadIDMap) {
    ByteArrayOutputStream bOutStream = new ByteArrayOutputStream(ZIP_BUFFER_INITIAL_SIZE);
    ZipOutputStream zout = new ZipOutputStream(bOutStream);
    ZipEntry zEntry = new ZipEntry(ZIP_BUFFER_NAME);
    try {
      zout.putNextEntry(zEntry);
    } catch (IOException e) {
      logger.error(e);
      return null;
    }

    String threadDump = getThreadDump(lockInfo, threadIDMap);
    logger.info(threadDump);

    try {
      zout.write(Conversion.string2Bytes(threadDump));
      zout.flush();
    } catch (IOException e) {
      logger.error(e);
      return null;
    } finally {
      try {
        zout.closeEntry();
        zout.close();
      } catch (IOException e) {
        logger.error(e);
        return null;
      }
    }

    return bOutStream.toByteArray();
  }

  public static String getThreadDump() {
    return getThreadDump(new NullLockInfoByThreadIDImpl(), new NullThreadIDMapImpl());
  }

  public static String getThreadDump(LockInfoByThreadID lockInfo, ThreadIDMap threadIDMap) {
    return ThreadDumpUtilJdk16.getThreadDump(lockInfo, threadIDMap);
  }

  public static String getLockList(LockInfoByThreadID lockInfo, ThreadID tcThreadID) {
    String lockList = "";
    ArrayList heldLocks = lockInfo.getHeldLocks(tcThreadID);
    ArrayList waitOnLocks = lockInfo.getWaitOnLocks(tcThreadID);
    ArrayList pendingLocks = lockInfo.getPendingLocks(tcThreadID);
    if (heldLocks.size() != 0) {
      lockList += "LOCKED : " + heldLocks.toString() + "\n";
    }
    if (waitOnLocks.size() != 0) {
      lockList += "WAITING ON LOCK: " + waitOnLocks.toString() + "\n";
    }
    if (pendingLocks.size() != 0) {
      lockList += "WAITING TO LOCK: " + pendingLocks.toString() + "\n";
    }
    return lockList;
  }

  /**
   * Get all threads.
   */
  public static Thread[] getAllThreads() {
    final ThreadGroup root = getRootThreadGroup();
    int alloc = threadMXBean.getThreadCount();
    int size = 0;
    Thread[] threads;
    // ThreadGroup.enumerate() will only return as many threads as it can fit in
    // the array it's given, and we have no accurate way to know how many threads
    // there will be at the time it is called.
    do {
      alloc *= 2;
      threads = new Thread[alloc];
      size = root.enumerate(threads, true);
    } while (size >= alloc);
    Thread[] trimmed = new Thread[size];
    System.arraycopy(threads, 0, trimmed, 0, size);
    return trimmed;
  }

  public static ThreadGroup getRootThreadGroup() {
    if (rootThreadGroup == null) {
      ThreadGroup tg = Thread.currentThread().getThreadGroup();
      ThreadGroup parent = tg.getParent();
      while (parent != null) {
        tg = parent;
        parent = tg.getParent();
      }
      rootThreadGroup = tg;
    }
    return rootThreadGroup;
  }

  public static void main(String[] args) {
    System.out.println(getThreadDump());
  }
}
