/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.process.Exec;
import com.tc.process.StreamCollector;
import com.tc.process.Exec.Result;
import com.tc.test.TestConfigObject;
import com.tc.text.Banner;
import com.tc.util.concurrent.ThreadUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    PID pid = getPID();
    dumpThreadsMany(iterations, delay, Collections.singleton(pid));
    return pid.getPid();
  }

  static PID getPID() {
    try {
      return new PID(GetPid.getInstance().getPid(), "not available");
    } catch (Exception e) {
      e.printStackTrace();

      // Use fallback mechanism
      return getPIDUsingFallback();
    }

    // unreachable
  }

  static PID getPIDUsingFallback() {
    String vmName = ManagementFactory.getRuntimeMXBean().getName();
    int index = vmName.indexOf('@');

    if (index < 0) { throw new RuntimeException("unexpected format: " + vmName); }

    return new PID(Integer.parseInt(vmName.substring(0, index)), "not available");
  }

  private static void dumpThreadsMany(int iterations, long delay, Set<PID> pids) {
    echoProcesses(pids);

    boolean multiple = pids.size() > 1;

    for (int i = 0; i < iterations; i++) {
      for (PID pid : pids) {
        System.err.println("Requesting dump for PID " + pid.getPid());

        if (Os.isWindows()) {
          doWindowsDump(pid);
        } else {
          doUnixDump(pid);
        }
        if (multiple) {
          // delay a bit to help prevent overlapped output
          ThreadUtil.reallySleep(50);
        }
      }
      ThreadUtil.reallySleep(delay);
    }
  }

  private static void echoProcesses(Set<PID> pids) {
    StringBuilder sb = new StringBuilder("Thread dumping these processes:\n");
    for (PID pid : pids) {
      sb.append("  ");
      sb.append(pid.getPid());
      sb.append("\t");
      sb.append(pid.getCmdLine());
      sb.append("\n");
    }

    System.err.println(sb.toString());

  }

  public static void dumpAllJavaProcesses() {
    dumpAllJavaProcesses(1, 0L);
  }

  public static void dumpAllJavaProcesses(int iterations, long delay) {
    dumpThreadsMany(iterations, delay, findAllJavaPIDs());
  }

  private static void doUnixDump(PID pid) {
    doSignal(new String[] { "-QUIT" }, pid);
  }

  private static void doWindowsDump(PID pid) {
    doSignal(new String[] {}, pid);
  }

  // private static void doIbmDump() throws ClassNotFoundException, SecurityException, NoSuchMethodException,
  // IllegalArgumentException, IllegalAccessException, InvocationTargetException {
  // final Class ibmDumpClass = Class.forName("com.ibm.jvm.Dump");
  // final Method ibmDumpMethod = ibmDumpClass.getDeclaredMethod("JavaDump", new Class[] {});
  // ibmDumpMethod.invoke(null, new Object[] {});
  // }

  private static void doSignal(String[] args, PID pid) {
    File program = SignalProgram.PROGRAM;

    try {
      String[] cmd = new String[1 + args.length + 1];
      cmd[0] = program.getAbsolutePath();
      System.arraycopy(args, 0, cmd, 1, args.length);

      cmd[cmd.length - 1] = String.valueOf(pid.getPid());

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

  static Set<PID> findAllJavaPIDs() {
    if (Os.isWindows()) {
      return windowsFindAllJavaPIDs();
    } else {
      return unixFindAllJavaPIDs();
    }
  }

  private static Set<PID> unixFindAllJavaPIDs() {
    Set<PID> pids = new HashSet<PID>();

    Result result;
    try {
      // XXX: We could support better filtering on solaris eventually using either /usr/ucb/ps and/or pargs
      // XXX: For now though we end up thread dumping all VMs (like we did before)
      String cmdArg = Os.isSolaris() ? "comm" : "command";
      result = Exec.execute(new String[] { "/bin/ps", "-eo", "pid,user," + cmdArg });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
      Pattern pattern = Pattern.compile("^(\\d+)\\s+(\\S+)\\s+(\\S+)(.*)$");

      String stdout = result.getStdout();
      BufferedReader reader = new BufferedReader(new StringReader(stdout));
      reader.readLine(); // skip header line

      String user = System.getProperty("user.name");
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();

        Matcher matcher = pattern.matcher(line);

        if (!matcher.matches()) {
          System.err.println("\nNON-MATCH: [" + line + "]\n");
          continue;
        }

        String pid = matcher.group(1);
        String uid = matcher.group(2);
        String cmd = matcher.group(3);
        String args = matcher.group(4);

        if (uid.equals(user) && cmd.endsWith("java")) {
          if (skip(args)) {
            continue;
          }

          boolean added = pids.add(new PID(Integer.parseInt(pid), line));
          if (!added) {
            Banner.warnBanner("Found duplicate PID? " + line);
          }
        }
      }

    } catch (Exception e) {
      throw new RuntimeException(result.toString(), e);
    }

    return pids;
  }

  private static Set<PID> windowsFindAllJavaPIDs() {
    Set<PID> pids = new HashSet<PID>();

    String pvExe = new File(TestConfigObject.getInstance().executableSearchPath(), "pv.exe").getAbsolutePath();

    Result result;
    try {
      result = Exec.execute(new String[] { pvExe, "-l", "-q", "java.exe" });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
      Pattern pattern = Pattern.compile("^(\\S+)\\s+(\\d+)\\s+(.*)$");

      String stdout = result.getStdout();
      if (!stdout.contains("No matching processes found")) {
        BufferedReader reader = new BufferedReader(new StringReader(stdout));

        String line;
        while ((line = reader.readLine()) != null) {

          Matcher matcher = pattern.matcher(line);

          if (!matcher.matches()) {
            System.err.println("\nNON-MATCH: [" + line + "]\n");
            continue;
          }

          String pid = matcher.group(2);
          String cmd = matcher.group(3);

          if (skip(cmd)) {
            continue;
          }

          boolean added = pids.add(new PID(Integer.parseInt(pid), line));
          if (!added) {
            Banner.warnBanner("Found duplicate PID? " + line);
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(result.toString(), e);
    }

    return pids;
  }

  private static boolean skip(String cmd) {
    // try to filter out VMs we don't want to thread dump
    return cmd.contains("-jar slave.jar") || cmd.contains("hudson.maven.agent.Main")
           || cmd.contains("cruisecontrol-launcher.jar ")
           || (cmd.contains(" org.jruby.Main ") && cmd.contains("build-tc.rb"))
           || (cmd.contains(" org.codehaus.classworlds.Launcher ") && cmd.contains("-Dmaven.home="));
  }

  static class PID {
    private final int    pid;
    private final String cmdLine;

    PID(int pid, String cmdLine) {
      this.pid = pid;
      this.cmdLine = cmdLine;
    }

    String getCmdLine() {
      return cmdLine;
    }

    int getPid() {
      return pid;
    }

    @Override
    public String toString() {
      return String.valueOf(pid) + " [" + cmdLine + "]";
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof PID)) { return false; }
      return ((PID) obj).pid == this.pid;
    }

    @Override
    public int hashCode() {
      return this.pid;
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
