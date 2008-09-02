/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.lang.reflect.Method;
import java.util.Map;

public class ThreadDumpUtil {

  private static final TCLogger logger            = TCLogging.getLogger(ThreadDumpUtil.class);

  private static Class          threadDumpUtilJdk15Type;
  private static Class          threadDumpUtilJdk16Type;
  private static final Class[]  EMPTY_PARAM_TYPES = new Class[0];
  private static final Object[] EMPTY_PARAMS      = new Object[0];

  static {
    if (Vm.isJDK15Compliant()) {
      try {
        threadDumpUtilJdk15Type = Class.forName("com.tc.util.runtime.ThreadDumpUtilJdk15");
      } catch (ClassNotFoundException cnfe) {
        logger.warn("Class ThreadDumpUtilJdk15 not found, java 1.5 thread dumps not available");
        threadDumpUtilJdk15Type = null;
      }

      try {
        threadDumpUtilJdk16Type = Class.forName("com.tc.util.runtime.ThreadDumpUtilJdk16");
      } catch (ClassNotFoundException cnfe) {
        logger.warn("Class ThreadDumpUtilJdk16 not found, java 1.6 thread dumps not available");
        threadDumpUtilJdk16Type = null;
      }
    } else {
      // Thread dumps require JRE-1.5 or greater
    }
  }

  public static String getThreadDump() {
    return getThreadDump(null, null, new NullThreadIDMap());
  }

  public static String getThreadDump(Map heldLockMap, Map pendingLockMap, ThreadIDMap threadIDMap) {
    final Exception exception;
    try {
      if (!Vm.isJDK15Compliant()) { return "Thread dumps require JRE-1.5 or greater"; }

      Method method = null;
      if (Vm.isJDK15()) {
        if (threadDumpUtilJdk15Type != null) {
          method = getThreadDumpMethod(threadDumpUtilJdk15Type, heldLockMap, pendingLockMap);
        } else {
          return "ThreadDump Classes class not available";
        }

      } else if (Vm.isJDK16Compliant()) {
        if (threadDumpUtilJdk16Type != null) {
          method = getThreadDumpMethod(threadDumpUtilJdk16Type, heldLockMap, pendingLockMap);
        } else if (threadDumpUtilJdk15Type != null) {
          method = getThreadDumpMethod(threadDumpUtilJdk15Type, heldLockMap, pendingLockMap);
        } else {
          return "ThreadDump Classes class not available";
        }
      } else {
        return "Thread dumps require JRE-1.5 or greater";
      }

      if ((heldLockMap != null) && (pendingLockMap != null)
          && (threadDumpUtilJdk15Type != null || threadDumpUtilJdk16Type != null)) {
        return (String) method.invoke(null, new Object[] { heldLockMap, pendingLockMap, threadIDMap });
      } else {
        return (String) method.invoke(null, EMPTY_PARAMS);
      }
    } catch (Exception e) {
      logger.error("Cannot take thread dumps - " + e.getMessage(), e);
      exception = e;
    }
    return "Cannot take thread dumps " + exception.getMessage();
  }

  private static Method getThreadDumpMethod(Class jdkType, Map heldLockMap, Map pendingLockMap) throws Exception {
    Method method = null;
    if (heldLockMap != null && pendingLockMap != null) {
      method = jdkType.getMethod("getThreadDump", new Class[] { Map.class, Map.class, ThreadIDMap.class });
    } else {
      method = jdkType.getMethod("getThreadDump", EMPTY_PARAM_TYPES);
    }
    return method;
  }

  public static String getHeldAndPendingLockInfo(Map heldMap, Map pendingMap, Long tcThreadID) {
    String info = "";
    if (heldMap != null && pendingMap != null) {

      Object heldLocks = heldMap.get(tcThreadID);
      Object pendingLocks = pendingMap.get(tcThreadID);

      if ((tcThreadID != null) && (heldLocks != null)) {
        info += "distributed locks HELD: " + heldLocks + "\n";
      }

      if ((tcThreadID != null) && (pendingLocks != null)) {
        info += "distributed locks WAITING for: " + pendingLocks + "\n";
      }

    }
    return info;
  }

  public static void main(String[] args) {
    System.out.println(getThreadDump());
  }
}