/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.util.Assert;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class ThreadDumpUtil {

  private static final TCLogger logger = TCLogging.getLogger(ThreadDumpUtil.class);

  private static Class          threadDumpUtilJdk15Type;
  private static Class          threadDumpUtilJdk16Type;

  static {
    if (Vm.isJDK15Compliant()) {
      try {
        threadDumpUtilJdk15Type = Class.forName("com.tc.util.runtime.ThreadDumpUtilJdk15");
      } catch (ClassNotFoundException cnfe) {
        logger.warn("Unable to load com.tc.util.runtime.ThreadDumpUtilJdk15", cnfe);
        threadDumpUtilJdk15Type = null;
      }

      if (Vm.isJDK16Compliant()) {
        try {
          threadDumpUtilJdk16Type = Class.forName("com.tc.util.runtime.ThreadDumpUtilJdk16");
        } catch (ClassNotFoundException cnfe) {
          logger.warn("Unable to load com.tc.util.runtime.ThreadDumpUtilJdk16", cnfe);
          threadDumpUtilJdk16Type = null;
        }
      }
    } else {
      // Thread dumps require JRE-1.5 or greater
    }
  }

  public static String getThreadDump() {
    return getThreadDump(new NullLockInfoByThreadIDImpl(), new NullThreadIDMap());
  }

  public static String getThreadDump(LockInfoByThreadID lockInfo, ThreadIDMap threadIDMap) {
    final Exception exception;
    try {
      if (!Vm.isJDK15Compliant()) { return "Thread dumps require JRE-1.5 or greater"; }
      Assert.assertTrue((threadDumpUtilJdk15Type != null) || (threadDumpUtilJdk16Type != null));

      Method method = null;
      if (Vm.isJDK15()) {
        if (threadDumpUtilJdk15Type != null) {
          method = getThreadDumpMethod(threadDumpUtilJdk15Type, lockInfo);
        } else {
          return "ThreadDump Classes class not available";
        }

      } else if (Vm.isJDK16Compliant()) {
        if (threadDumpUtilJdk16Type != null) {
          method = getThreadDumpMethod(threadDumpUtilJdk16Type, lockInfo);
        } else if (threadDumpUtilJdk15Type != null) {
          method = getThreadDumpMethod(threadDumpUtilJdk15Type, lockInfo);
        }
      } else {
        return "Thread dumps require JRE-1.5 or greater";
      }
      return (String) method.invoke(null, new Object[] { lockInfo, threadIDMap });
    } catch (Exception e) {
      logger.error("Cannot take thread dumps - " + e.getMessage(), e);
      exception = e;
    }
    return "Cannot take thread dumps " + exception.getMessage();
  }

  private static Method getThreadDumpMethod(Class jdkType, LockInfoByThreadID lockInfo) throws Exception {
    Method method = null;
    Assert.assertNotNull(lockInfo);
    method = jdkType.getMethod("getThreadDump", new Class[] { LockInfoByThreadID.class, ThreadIDMap.class });
    return method;
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

  public static void main(String[] args) {
    System.out.println(getThreadDump());
  }
}