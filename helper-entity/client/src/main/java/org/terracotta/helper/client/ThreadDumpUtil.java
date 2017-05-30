/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.helper.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;

public class ThreadDumpUtil {

  private static final Logger       LOGGER         = LoggerFactory.getLogger(ThreadDumpUtil.class); 
  private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
  

  public static void getThreadDump() {
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
      final ThreadInfo[] threadsInfo = THREAD_MX_BEAN.dumpAllThreads(THREAD_MX_BEAN.isObjectMonitorUsageSupported(),
                                                                   THREAD_MX_BEAN.isSynchronizerUsageSupported());

      for (final ThreadInfo threadInfo : threadsInfo) {
        threadHeader(sb, threadInfo);

        final StackTraceElement[] stea = threadInfo.getStackTrace();
        final MonitorInfo[] monitorInfos = threadInfo.getLockedMonitors();
        for (StackTraceElement element : stea) {
          sb.append("\tat ");
          sb.append(element.toString());
          sb.append('\n');
          for (final MonitorInfo monitorInfo : monitorInfos) {
            final StackTraceElement lockedFrame = monitorInfo.getLockedStackFrame();
            if (lockedFrame != null && lockedFrame.equals(element)) {
              sb.append("\t- locked <0x");
              sb.append(Integer.toHexString(monitorInfo.getIdentityHashCode()));
              sb.append("> (a ");
              sb.append(monitorInfo.getClassName());
              sb.append(")");
              sb.append('\n');
            }
          }
        }
        if (!THREAD_MX_BEAN.isObjectMonitorUsageSupported() && THREAD_MX_BEAN.isSynchronizerUsageSupported()) {
          sb.append(threadLockedSynchronizers(threadInfo));
        }
        sb.append('\n');
      }
    } catch (final Exception e) {
      LOGGER.error("Cannot take thread dumps - " + e.getMessage(), e);
      sb.append(e.toString());
    }
    LOGGER.info(sb.toString());
  }

  private static void threadHeader(StringBuilder sb, ThreadInfo threadInfo) {
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

  private static String threadLockedSynchronizers(ThreadInfo threadInfo) {
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
}
