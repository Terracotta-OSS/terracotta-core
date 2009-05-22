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

  public static String getThreadDump(LockInfoByThreadID lockInfo, ThreadIDMap threadIDMap) {

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
          sb.append(threadHeader(thread));
          sb.append('\n');
          StackTraceElement[] stea = thread.getStackTrace();
          for (int j = 0; j < stea.length; j++) {
            sb.append("\tat ");
            sb.append(stea[j].toString());
            sb.append('\n');
          }
          sb.append(ThreadDumpUtil.getLockList(lockInfo, threadIDMap.getTCThreadID(thread)));
          sb.append('\n');
        }
    } catch (Exception e) {
      e.printStackTrace();
      sb.append(e.toString());
    }
    return sb.toString();
  }
  
  private static String threadHeader(Thread thread) {
    long threadId = thread.getId();
    // CDV-1262: If Thread.getId() has been overridden, this may return null 
    // or even data from a different thread.
    ThreadInfo threadInfo = threadMXBean.getThreadInfo(thread.getId(), Integer.MAX_VALUE);
    String threadName = thread.getName();
    StringBuffer sb = new StringBuffer();
    sb.append("\"");
    sb.append(threadName);
    sb.append("\" ");
    sb.append("Id=");
    sb.append(threadId);

    try {

      if (threadInfo != null) {
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
      } else {
        sb.append(" (unrecognized thread id; thread state is unavailable)");
      }

      return sb.toString();
    } catch (Exception e) {
      return threadInfo.toString();
    }
  }


}
