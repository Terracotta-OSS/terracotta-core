/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.lang.reflect.Method;
import java.util.Date;

public class ThreadDumpUtil {

  public static final TCLogger logger = TCLogging.getLogger(ThreadDumpUtil.class);

  private static Class managementFactoryType;

  private static final Class[] EMPTY_PARAM_TYPES = new Class[0];
  private static final Object[] EMPTY_PARAMS = new Object[0];
  private static Integer ALL_FRAMES = new Integer(Integer.MAX_VALUE);

  private static Object threadBean;
  private static ThreadBeanMethods threadBeanMethods;
  private static ThreadInfoMethods threadInfoMethods;


  public static String getThreadDump() {
    if (managementFactoryType == null) {
      try {
        managementFactoryType = Class.forName("java.lang.management.ManagementFactory");
      } catch (ClassNotFoundException cnfe) {
        return "Thread dumps require JRE-1.5 or greater";
      }
    }

    if (managementFactoryType == null) { return "Cannot find java.lang.management.ManagementFactory"; }

    StringBuffer sb = new StringBuffer();

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
      long[] threadIds = getAllThreadIds();

      for (int i = 0; i < threadIds.length; i++) {
        Object threadInfo = getThreadInfo(threadIds[i]);

        if (threadInfo != null) {
          String s = threadInfo.toString();

          if (s.indexOf('\n') == -1) { // 1.5
            sb.append(threadHeader(threadInfo, threadIds[i]));
            sb.append('\n');

            StackTraceElement[] stea = getStackTrace(threadInfo);
            for (int j = 0; j < stea.length; j++) {
              sb.append("\tat ");
              sb.append(stea[j].toString());
              sb.append('\n');
            }
            sb.append('\n');
          } else {
            sb.append(s);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      sb.append(e.toString());
    }

    String text = sb.toString();
    //logger.info(text);

    return text;
  }

  private static Object getThreadMXBean() throws Exception {
    if (threadBean != null) return threadBean;

    Method threadBeanGetter = managementFactoryType.getMethod("getThreadMXBean", EMPTY_PARAM_TYPES);
    threadBean = threadBeanGetter.invoke(null, EMPTY_PARAMS);
    return threadBean;
  }

  private static long[] getAllThreadIds() throws Exception {
    Object threadMXBean = getThreadMXBean();
    ThreadBeanMethods methods = getThreadBeanMethods();
    return (long[])methods.threadIdsGetter.invoke(threadMXBean, EMPTY_PARAMS);
  }

  private static ThreadBeanMethods getThreadBeanMethods() throws Exception {
    if (threadBeanMethods != null) return threadBeanMethods;

    threadBeanMethods = new ThreadBeanMethods();
    Object threadMXBean = getThreadMXBean();
    Class threadMXBeanType = threadMXBean.getClass();
    threadBeanMethods.threadIdsGetter = threadMXBeanType.getMethod("getAllThreadIds", EMPTY_PARAM_TYPES);
    threadBeanMethods.threadIdsGetter.setAccessible(true);
    threadBeanMethods.threadInfoGetter = threadMXBeanType.getMethod("getThreadInfo", new Class[] { Long.TYPE,
      Integer.TYPE });
    threadBeanMethods.threadInfoGetter.setAccessible(true);
    return threadBeanMethods;
  }

  private static ThreadInfoMethods getThreadInfoMethods(Object threadInfo) throws Exception {
    if (threadInfoMethods != null) return threadInfoMethods;

    threadInfoMethods = new ThreadInfoMethods();
    Class threadInfoType = threadInfo.getClass();
    threadInfoMethods.stackTraceGetter = threadInfoType.getMethod("getStackTrace", EMPTY_PARAM_TYPES);
    threadInfoMethods.threadNameGetter = threadInfoType.getMethod("getThreadName", EMPTY_PARAM_TYPES);
    threadInfoMethods.threadStateGetter = threadInfoType.getMethod("getThreadState", EMPTY_PARAM_TYPES);
    threadInfoMethods.lockNameGetter = threadInfoType.getMethod("getLockName", EMPTY_PARAM_TYPES);
    threadInfoMethods.lockOwnerNameGetter = threadInfoType.getMethod("getLockOwnerName", EMPTY_PARAM_TYPES);
    threadInfoMethods.lockOwnerIdGetter = threadInfoType.getMethod("getLockOwnerId", EMPTY_PARAM_TYPES);
    threadInfoMethods.isSuspendedGetter = threadInfoType.getMethod("isSuspended", EMPTY_PARAM_TYPES);
    threadInfoMethods.isInNativeGetter = threadInfoType.getMethod("isInNative", EMPTY_PARAM_TYPES);
    return threadInfoMethods;
  }

  private static String threadHeader(Object threadInfo, long threadId) {
    try {
      ThreadInfoMethods methods = getThreadInfoMethods(threadInfo);

      String threadName = (String)methods.threadNameGetter.invoke(threadInfo, EMPTY_PARAMS);
      Object threadState = methods.threadStateGetter.invoke(threadInfo, EMPTY_PARAMS);
      String lockName = (String)methods.lockNameGetter.invoke(threadInfo, EMPTY_PARAMS);
      String lockOwnerName = (String)methods.lockOwnerNameGetter.invoke(threadInfo, EMPTY_PARAMS);
      Long lockOwnerId = (Long)methods.lockOwnerIdGetter.invoke(threadInfo, EMPTY_PARAMS);
      Boolean isSuspended = (Boolean)methods.isSuspendedGetter.invoke(threadInfo, EMPTY_PARAMS);
      Boolean isInNative = (Boolean)methods.isInNativeGetter.invoke(threadInfo, EMPTY_PARAMS);

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
      if (isSuspended.booleanValue()) {
        sb.append(" (suspended)");
      }
      if (isInNative.booleanValue()) {
        sb.append(" (in native)");
      }

      return sb.toString();
    } catch (Exception e) {
      return threadInfo.toString();
    }
  }

  private static Object getThreadInfo(long threadId) throws Exception {
    Object threadMXBean = getThreadMXBean();
    ThreadBeanMethods methods = getThreadBeanMethods();
    return methods.threadInfoGetter.invoke(threadMXBean, new Object[] { new Long(threadId), ALL_FRAMES });
  }

  private static StackTraceElement[] getStackTrace(Object threadInfo) throws Exception {
    ThreadInfoMethods methods = getThreadInfoMethods(threadInfo);
    return (StackTraceElement[])methods.stackTraceGetter.invoke(threadInfo, EMPTY_PARAMS);
  }
}

class ThreadBeanMethods {
  Method threadIdsGetter;
  Method threadInfoGetter;
}

class ThreadInfoMethods {
  Method stackTraceGetter;
  Method threadNameGetter;
  Method threadStateGetter;
  Method lockNameGetter;
  Method lockOwnerNameGetter;
  Method lockOwnerIdGetter;
  Method isSuspendedGetter;
  Method isInNativeGetter;
}
