/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;

public class ThreadDumpUtilJdk15 {

  private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

  public static String getThreadDump() {
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
      long[] threadIds = threadMXBean.getAllThreadIds();

      for (int i = 0; i < threadIds.length; i++) {
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadIds[i], Integer.MAX_VALUE);
        if (threadInfo != null) {
          String s = threadInfo.toString();

          sb.append(threadHeader(threadInfo, threadIds[i]));
          sb.append('\n');

          StackTraceElement[] stea = threadInfo.getStackTrace();
          for (int j = 0; j < stea.length; j++) {
            sb.append("\tat ");
            sb.append(stea[j].toString());
            sb.append('\n');
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

  private static String threadHeader(ThreadInfo threadInfo, long threadId) {
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
      sb.append(threadId);
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

  public static void main(String[] args) {
    System.out.println(getThreadDump());
  }
}
