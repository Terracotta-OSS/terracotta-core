/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.exception.TCRuntimeException;
import com.tc.process.StreamCollector;
import com.tc.test.TestConfigObject;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.exception.ExceptionUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ThreadDump {

  public static void main(String args[]) {
    dumpThreadsOnce();

    // This flush()'ing and sleeping is a (perhaps poor) attempt at making ThreadDumpTest pass on Jrockit
    System.err.flush();
    System.out.flush();
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void dumpThreadsOnce() {
    dumpThreadsMany(1, 0L);
  }

  public static int dumpThreadsMany(int iterations, long delay) {
    int pid = 0;
    RuntimeException windowsPidException = null;
    try {
      pid = GetPid.getPID();
    } catch (RuntimeException re) {
      if (Os.isWindows()) {
        if (Vm.isIBM()) {
          windowsPidException = re;
        } else {
          throw re;
        }
      } else {
        System.err.println("Got exception trying to get the process ID, stacktrace is below:");
        ExceptionUtil.dumpFullStackTrace(re, System.err);
        System.err.flush();
      }
    }

    for (int i = 0; i < iterations; i++) {
      boolean doStandardDump = !Vm.isIBM();
      if (Vm.isIBM()) {
        try {
          doIbmDump();
        } catch (Exception e) {
          System.err.println("Got an exception while trying to use the native IBM thread"
              + " dump facility, using 'standard' method.  Stacktrace is below:");
          ExceptionUtil.dumpFullStackTrace(e, System.err);
          System.err.flush();
          doStandardDump = true;
        }
      }
      if (doStandardDump) {
        if (pid == 0) {
          if (Os.isWindows()) {
            throw new TCRuntimeException("Unable to find my process ID, cannot dump threads", windowsPidException);
          } else {
            System.err.println("My PID is not available, sending QUIT signal to process group (PID 0)");
            System.err.flush();
          }
        }
        if (Os.isWindows()) {
          doWindowsDump(pid);
        } else {
          doUnixDump(pid);
        }
      }
      ThreadUtil.reallySleep(delay);
    }

    return pid;
  }

  public static void dumpProcessGroup() {
    if (!Os.isUnix()) { throw new AssertionError("unix only"); }
    doUnixDump(0);
  }

  private static void doUnixDump(int pid) {
    doSignal(new String[] { "-QUIT" }, pid);
  }

  private static void doWindowsDump(int pid) {
    doSignal(new String[] {}, pid);
  }

  private static void doIbmDump() throws ClassNotFoundException, SecurityException, NoSuchMethodException,
      IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    final Class ibmDumpClass = Class.forName("com.ibm.jvm.Dump");
    final Method ibmDumpMethod = ibmDumpClass.getDeclaredMethod("JavaDump", new Class[] {});
    ibmDumpMethod.invoke(null, new Object[] {});
  }

  private static void doSignal(String[] args, int pid) {
    File program = SignalProgram.PROGRAM;

    try {
      String[] cmd = new String[1 + args.length + 1];
      cmd[0] = program.getAbsolutePath();
      System.arraycopy(args, 0, cmd, 1, args.length);

      cmd[cmd.length - 1] = Integer.toString(pid);

      Process p = Runtime.getRuntime().exec(cmd);
      p.getOutputStream().close();
      StreamCollector err = new StreamCollector(p.getErrorStream());
      StreamCollector out = new StreamCollector(p.getInputStream());
      err.start();
      out.start();

      p.waitFor();

      err.join();
      out.join();

      System.err.print(err.toString());
      System.err.flush();
      System.out.print(out.toString());
      System.out.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static class SignalProgram {
    static final File PROGRAM;

    static {
      PROGRAM = getSignalProgram();
    }

    private static File getSignalProgram() {
      File rv = null;

      if (Os.isWindows()) {
        try {
          rv = new File(TestConfigObject.getInstance().executableSearchPath(), "SendSignal.EXE");
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        File binKill = new File("/bin/kill");
        File usrBinKill = new File("/usr/bin/kill");

        if (binKill.exists()) {
          rv = binKill;
        } else if (usrBinKill.exists()) {
          rv = usrBinKill;
        }
      }

      if (rv != null) {
        if (rv.exists() && rv.isFile()) { return rv; }
        System.err.println("Cannot find signal program: " + rv.getAbsolutePath());
        System.err.flush();
      }

      throw new RuntimeException("Cannot find signal program");
    }
  }
}
