/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.Date;

public class ThreadDumpUtilJdk16 extends ThreadDumpUtil {

  public static String getThreadDump() {
    return getThreadDump(new NullLockInfoByThreadIDImpl(), new NullThreadIDMapImpl());
  }

  public static String getThreadDump(LockInfoByThreadID lockInfo, ThreadIDMap threadIDMap) {
    final StringBuilder sb = new StringBuilder(100 * 1024);
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
      final ThreadInfo[] threadsInfo = threadMXBean.dumpAllThreads(threadMXBean.isObjectMonitorUsageSupported(),
                                                                   threadMXBean.isSynchronizerUsageSupported());

      for (final ThreadInfo threadInfo : threadsInfo) {
        threadHeader(sb, threadInfo);

        final StackTraceElement[] stea = threadInfo.getStackTrace();
        final MonitorInfo[] monitorInfos = threadInfo.getLockedMonitors();
        for (int j = 0; j < stea.length; j++) {
          sb.append("\tat ");
          sb.append(stea[j].toString());
          sb.append('\n');
          for (final MonitorInfo monitorInfo : monitorInfos) {
            final StackTraceElement lockedFrame = monitorInfo.getLockedStackFrame();
            if (lockedFrame != null && lockedFrame.equals(stea[j])) {
              sb.append("\t- locked <0x");
              sb.append(Integer.toHexString(monitorInfo.getIdentityHashCode()));
              sb.append("> (a ");
              sb.append(monitorInfo.getClassName());
              sb.append(")");
              sb.append('\n');
            }
          }
        }
        sb.append(ThreadDumpUtil.getLockList(lockInfo, threadIDMap.getTCThreadID(threadInfo.getThreadId())));
        if (!threadMXBean.isObjectMonitorUsageSupported() && threadMXBean.isSynchronizerUsageSupported()) {
          sb.append(threadLockedSynchronizers(threadInfo));
        }
        sb.append('\n');
      }
    } catch (final Exception e) {
      e.printStackTrace();
      sb.append(e.toString());
    }
    return sb.toString();
  }

  private static void threadHeader(final StringBuilder sb, final ThreadInfo threadInfo) {
    final String threadName = threadInfo.getThreadName();
    sb.append("\"");
    sb.append(threadName);
    sb.append("\" ");
    sb.append("Id=");
    sb.append(threadInfo.getThreadId());

    try {
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
    } catch (final Exception e) {
      sb.append(" ( Got exception : ").append(e.getMessage()).append(" :");
    }

    sb.append('\n');
  }

  private static String threadLockedSynchronizers(final ThreadInfo threadInfo) {
    final String NO_SYNCH_INFO = "no locked synchronizers information available\n";
    if (null == threadInfo) { return NO_SYNCH_INFO; }
    try {
      final LockInfo[] lockInfos = threadInfo.getLockedSynchronizers();
      if (lockInfos.length > 0) {
        final StringBuffer lockedSynchBuff = new StringBuffer();
        lockedSynchBuff.append("\nLocked Synchronizers: \n");
        for (final LockInfo lockInfo : lockInfos) {
          lockedSynchBuff.append(lockInfo.getClassName()).append(" <").append(lockInfo.getIdentityHashCode())
              .append("> \n");
        }
        return lockedSynchBuff.append("\n").toString();
      } else {
        return "";
      }
    } catch (final Exception e) {
      return NO_SYNCH_INFO;
    }
  }

  public static void main(final String[] args) {
    System.out.println(getThreadDump());
  }

}
