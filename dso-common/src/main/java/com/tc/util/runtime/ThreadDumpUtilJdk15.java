/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import java.lang.management.ThreadInfo;
import java.util.Date;

public class ThreadDumpUtilJdk15 extends ThreadDumpUtil {

  public static String getThreadDump() {
    return getThreadDump(new NullLockInfoByThreadIDImpl(), new NullThreadIDMapImpl());
  }

  public static String getThreadDump(final LockInfoByThreadID lockInfo, final ThreadIDMap threadIDMap) {

    final StringBuilder sb = new StringBuilder();
    sb.append(new Date().toString());
    sb.append('\n');
    sb.append("Full thread dump ");
    sb.append(System.getProperty("java.vm.name"));
    sb.append(" (");
    sb.append(System.getProperty("java.vm.version"));
    sb.append(' ');
    sb.append(System.getProperty("java.vm.info"));
    sb.append("):\n\n");
    try {
      final Thread[] threads = ThreadDumpUtil.getAllThreads();

      for (final Thread thread : threads) {
        threadHeader(sb, thread);
        sb.append('\n');
        final StackTraceElement[] stea = thread.getStackTrace();
        for (final StackTraceElement element : stea) {
          sb.append("\tat ");
          sb.append(element.toString());
          sb.append('\n');
        }
        sb.append(ThreadDumpUtil.getLockList(lockInfo, threadIDMap.getTCThreadID(thread.getId())));
        sb.append('\n');
      }
    } catch (final Exception e) {
      e.printStackTrace();
      sb.append(e.toString());
    }
    return sb.toString();
  }

  private static void threadHeader(final StringBuilder sb, final Thread thread) {
    final long threadId = thread.getId();
    // CDV-1262: If Thread.getId() has been overridden, this may return null
    // or even data from a different thread.
    final ThreadInfo threadInfo = threadMXBean.getThreadInfo(thread.getId(), Integer.MAX_VALUE);
    final String threadName = thread.getName();
    sb.append("\"");
    sb.append(threadName);
    sb.append("\" ");
    sb.append("Id=");
    sb.append(threadId);

    try {

      if (threadInfo != null) {
        final Thread.State threadState = threadInfo.getThreadState();
        final String lockName = threadInfo.getLockName();
        final String lockOwnerName = threadInfo.getLockOwnerName();
        final Long lockOwnerId = threadInfo.getLockOwnerId();
        final Boolean isSuspended = threadInfo.isSuspended();
        final Boolean isInNative = threadInfo.isInNative();

        sb.append(" ");
        sb.append(threadState);
        if (lockName != null) {
          sb.append(" on ");
          sb.append(lockName);
        }
        if (lockOwnerName != null) {
          sb.append(" owned by \"");
          sb.append(lockOwnerName);
          sb.append("\" Id=");
          sb.append(lockOwnerId);
        }
        if (isSuspended) {
          sb.append(" (suspended)");
        }
        if (isInNative) {
          sb.append(" (in native)");
        }
      } else {
        sb.append(" (unrecognized thread id; thread state is unavailable)");
      }

    } catch (final Exception e) {
      sb.append(threadInfo.toString());
    }
  }

}
