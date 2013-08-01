/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.test.util;

import static org.terracotta.test.util.JvmProcessUtil.sendSignal;

import org.terracotta.test.util.JvmProcessUtil.Signal;

import com.tc.lcp.LinkedJavaProcess;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

public class TestProcessUtil {
  
  public static void pauseProcess(LinkedJavaProcess process, long pauseTimeMillis) throws InterruptedException {
    int pid = getPid(process);
    if (pid < 0) {
      LogUtil.info(TestProcessUtil.class, "PID negative Cannot Pause a Process :" + process.toString());
      return;
    }
    try {
      sendSignal(Signal.SIGSTOP, pid);
      long time = System.currentTimeMillis();
      LogUtil.debug(TestProcessUtil.class, "Pausing Process PID : " + pid);
      try {
        TimeUnit.MILLISECONDS.sleep(pauseTimeMillis);
      } finally {
        sendSignal(Signal.SIGCONT, pid);
        LogUtil.debug(TestProcessUtil.class, "Resuming Process PID : " + pid + " in "
                                             + (System.currentTimeMillis() - time));
      }
    } catch (IOException e) {
      LogUtil.info(TestProcessUtil.class, "Pause : failed for PID : " + pid + " Becoz of " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void pauseProcess(LinkedJavaProcess process) throws InterruptedException {
    int pid = getPid(process);
    if (pid < 0) {
      LogUtil.info(TestProcessUtil.class, "PID negative Cannot Pause a Process :" + process.toString());
      return;
    }
    try {
      sendSignal(Signal.SIGSTOP, pid);
      LogUtil.debug(TestProcessUtil.class, "Pausing Process PID : " + pid);
    } catch (IOException e) {
      LogUtil.info(TestProcessUtil.class, "Pause : failed for PID : " + pid + " Becoz of " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void unpauseProcess(LinkedJavaProcess process) throws InterruptedException {
    int pid = getPid(process);
    if (pid < 0) {
      LogUtil.info(TestProcessUtil.class, "PID negative Cannot Pause a Process :" + process.toString());
      return;
    }
    try {
      sendSignal(Signal.SIGCONT, pid);
      LogUtil.debug(TestProcessUtil.class, "Resuming Process PID : " + pid);
    } catch (IOException e) {
      LogUtil.info(TestProcessUtil.class, "UnPause : failed for PID : " + pid + " Becoz of " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Needed a Good Implementation here. This is not good
   */
  private static int getPid(LinkedJavaProcess process) {
    try {
      Field processField = LinkedJavaProcess.class.getDeclaredField("process");
      processField.setAccessible(true);
      Object internalProcessObject = processField.get(process);
      Field pidField = internalProcessObject.getClass().getDeclaredField("pid");
      pidField.setAccessible(true);
      int pid = (Integer) pidField.get(internalProcessObject);
      return pid;
    } catch (Exception e) {
      LogUtil.info(TestProcessUtil.class, "Pause : failed for Process : " + process + " Becoz of " + e.getMessage());
      e.printStackTrace();
    }
    return -1;
  }

}
