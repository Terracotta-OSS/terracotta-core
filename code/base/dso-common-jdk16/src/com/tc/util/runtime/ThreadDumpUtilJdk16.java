/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.Map;

public abstract class ThreadDumpUtilJdk16 {

  private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

  public static String getThreadDump() {
    return getThreadDump(null, null, new NullThreadIDMap());
  }

  public static String getThreadDump(Map heldMap, Map pendingMap, NullThreadIDMap thIDMap) {
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
      ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

      for (int i = 0; i < threadInfos.length; i++) {
        ThreadInfo threadInfo = threadInfos[i];
        if (threadInfo != null) {
          sb.append(threadHeader(threadInfo));
          sb.append('\n');

          StackTraceElement[] stea = threadInfo.getStackTrace();
          for (int j = 0; j < stea.length; j++) {
            sb.append("\tat ");
            sb.append(stea[j].toString());
            sb.append('\n');
          }
          sb.append(ThreadDumpUtil.getHeldAndPendingLockInfo(heldMap, pendingMap, thIDMap.getTCThreadID(threadInfo
              .getThreadId())));
          if (threadMXBean.isObjectMonitorUsageSupported()) {
            sb.append("\n").append(threadLockedMonitors(threadInfo));
          } else if (threadMXBean.isSynchronizerUsageSupported()) {
            sb.append("\n").append(threadLockedSynchronizers(threadInfo));
          }
          sb.append('\n');
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      sb.append(e.toString());
    }
    return sb.toString();
  }

  private static String threadHeader(ThreadInfo threadInfo) {
    try {
      String threadName = threadInfo.getThreadName();
      Thread.State threadState = threadInfo.getThreadState();
      String lockName = threadInfo.getLockName();
      String lockOwnerName = threadInfo.getLockOwnerName();
      Long lockOwnerId = threadInfo.getLockOwnerId();
      Boolean isSuspended = threadInfo.isSuspended();
      Boolean isInNative = threadInfo.isInNative();

      StringBuffer sb = new StringBuffer();
      sb.append("\"");
      sb.append(threadName);
      sb.append("\" ");
      sb.append("Id=");
      sb.append(threadInfo.getThreadId());
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

      return sb.toString();
    } catch (Exception e) {
      return threadInfo.toString();
    }
  }

  private static String threadLockedSynchronizers(ThreadInfo threadInfo) {
    try {
      StringBuffer lockedSynchBuff = new StringBuffer();
      lockedSynchBuff.append("Locked Synchronizers: \n");
      LockInfo[] lockInfos = threadInfo.getLockedSynchronizers();
      if (lockInfos.length < 1) { return lockedSynchBuff.append("None \n").toString(); }
      for (int i = 0; i < lockInfos.length; i++) {
        LockInfo lockInfo = lockInfos[i];
        lockedSynchBuff.append(lockInfo.getClassName()).append(" <").append(lockInfo.getIdentityHashCode())
            .append("> \n");
      }
      return lockedSynchBuff.append("\n").toString();
    } catch (Exception e) {
      return "No Locked Synchronizers information available. \n";
    }
  }

  private static String threadLockedMonitors(ThreadInfo threadInfo) {
    try {
      StringBuffer lockedMonitorBuff = new StringBuffer();
      lockedMonitorBuff.append("Locked Monitors: \n");
      MonitorInfo[] monitorInfos = threadInfo.getLockedMonitors();
      if (monitorInfos.length < 1) { return lockedMonitorBuff.append("None \n").toString(); }
      for (int i = 0; i < monitorInfos.length; i++) {
        MonitorInfo monitorInfo = monitorInfos[i];
        lockedMonitorBuff.append(monitorInfo.getClassName()).append(" <").append(monitorInfo.getIdentityHashCode())
            .append("> \n");
        lockedMonitorBuff.append("lockedStackDepth: " + monitorInfo.getLockedStackDepth() + "\n");
        StackTraceElement stea = monitorInfo.getLockedStackFrame();
        lockedMonitorBuff.append("\tat ");
        lockedMonitorBuff.append(stea.toString());
        lockedMonitorBuff.append('\n');
      }
      return lockedMonitorBuff.append("\n").toString();
    } catch (Exception e) {
      return "No Locked monitors information available. \n";
    }
  }

  public static void main(String[] args) {
    System.out.println(getThreadDump());
  }

}
