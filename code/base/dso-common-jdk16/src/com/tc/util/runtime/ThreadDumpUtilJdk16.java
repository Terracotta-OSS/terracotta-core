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
    MonitorInfo[] emptyMI = new MonitorInfo[0];
    StringBuilder sb = new StringBuilder();
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
      Thread[] threads = ThreadDumpUtil.getAllThreads();

      for (Thread thread : threads) {
        long id = thread.getId();
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(new long[] {id}, true, true)[0];
        sb.append(threadHeader(thread, threadInfo));
        sb.append('\n');

        StackTraceElement[] stea = thread.getStackTrace();
        MonitorInfo[] monitorInfos = threadInfo != null ? threadInfo.getLockedMonitors() : emptyMI;
        for (int j = 0; j < stea.length; j++) {
          sb.append("\tat ");
          sb.append(stea[j].toString());
          sb.append('\n');
          for (MonitorInfo monitorInfo : monitorInfos) {
            if (monitorInfo.getLockedStackFrame().equals(stea[j])) {
              sb.append("\t- locked <0x");
              sb.append(Integer.toHexString(monitorInfo.getIdentityHashCode()));
              sb.append("> (a ");
              sb.append(monitorInfo.getClassName());
              sb.append(")");
              sb.append('\n');
            }
          }
        }
        sb.append(ThreadDumpUtil.getLockList(lockInfo, threadIDMap.getTCThreadID(thread)));
        if (!threadMXBean.isObjectMonitorUsageSupported() && threadMXBean.isSynchronizerUsageSupported()) {
          sb.append(threadLockedSynchronizers(threadInfo));
        }
        sb.append('\n');
      }
    } catch (Exception e) {
      e.printStackTrace();
      sb.append(e.toString());
    }
    return sb.toString();
  }

  private static String threadHeader(Thread thread, ThreadInfo threadInfo) {
    String threadName = thread.getName();
    StringBuffer sb = new StringBuffer();
    sb.append("\"");
    sb.append(threadName);
    sb.append("\" ");
    sb.append("Id=");
    sb.append(thread.getId());

    if (threadInfo != null) {
      try {
        Thread.State threadState = threadInfo.getThreadState();
        String lockName = threadInfo.getLockName();
        String lockOwnerName = threadInfo.getLockOwnerName();
        Long lockOwnerId = threadInfo.getLockOwnerId();
        Boolean isSuspended = threadInfo.isSuspended();
        Boolean isInNative = threadInfo.isInNative();

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
      } catch (Exception e) {
        return threadInfo.toString();
      }
    } else {
      sb.append(" (unrecognized thread id; thread state is unavailable)");
    }

    return sb.toString();
  }

  private static String threadLockedSynchronizers(ThreadInfo threadInfo) {
    final String NO_SYNCH_INFO = "no locked synchronizers information available\n";
    if (null == threadInfo) {
      return NO_SYNCH_INFO;
    }
    try {
      LockInfo[] lockInfos = threadInfo.getLockedSynchronizers();
      if (lockInfos.length > 0) {
        StringBuffer lockedSynchBuff = new StringBuffer();
        lockedSynchBuff.append("\nLocked Synchronizers: \n");
        for (int i = 0; i < lockInfos.length; i++) {
          LockInfo lockInfo = lockInfos[i];
          lockedSynchBuff.append(lockInfo.getClassName()).append(" <").append(lockInfo.getIdentityHashCode())
              .append("> \n");
        }
        return lockedSynchBuff.append("\n").toString();
      } else {
        return "";
      }
    } catch (Exception e) {
      return NO_SYNCH_INFO;
    }
  }

  public static void main(String[] args) {
    System.out.println(getThreadDump());
  }

}
